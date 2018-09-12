package org.bloqly.machine.service;

import com.google.common.collect.Lists;
import org.bloqly.machine.component.PropertyContext;
import org.bloqly.machine.function.GetPropertyFunction;
import org.bloqly.machine.model.Contract;
import org.bloqly.machine.model.InvocationContext;
import org.bloqly.machine.model.InvocationResult;
import org.bloqly.machine.model.PropertyResult;
import org.bloqly.machine.util.CryptoUtils;
import org.bloqly.machine.vo.property.PropertyValue;
import org.bloqly.machine.vo.property.Value;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.script.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.bloqly.machine.model.InvocationResultType.SUCCESS;

@Service
public class ContractExecutorService {

    @Autowired
    private ContractService contractService;

    private Map<String, ScriptEngine> engines = new ConcurrentHashMap<>();

    @PostConstruct
    @Transactional
    public void init() {
        contractService.findAll().forEach(this::initContract);
    }

    private void initContract(Contract contract) {
        getEngine(contract.getBody());
    }

    private GetPropertyFunction getPropertyFunction(PropertyContext propertyContext, InvocationContext invocationContext) {

        return (target, key, defaultValue) -> {

            var propertyValue = propertyContext.findPropertyValue(
                    invocationContext.getSpace(),
                    invocationContext.getSelf(),
                    target,
                    key
            );

            if (propertyValue != null) {
                return propertyValue.toValue();
            } else {
                return defaultValue;
            }
        };
    }

    private Invocable getEngine(PropertyContext propertyContext, InvocationContext invocationContext) {

        var contract = propertyContext.getContract(invocationContext.getSelf());

        Objects.requireNonNull(contract, "Could not find contract self: " + invocationContext.getSelf());

        invocationContext.setOwner(contract.getOwner());

        var engine = getEngine(contract.getBody());

        engine.put("getProperty", getPropertyFunction(propertyContext, invocationContext));

        return (Invocable) engine;
    }

    private ScriptEngine getEngine(String body) {

        var key = Hex.toHexString(CryptoUtils.INSTANCE.hash(body));

        engines.computeIfAbsent(key, (keyToCompute) -> {
            try {
                System.setProperty("nashorn.args", "--language=es6");

                var engine = new ScriptEngineManager().getEngineByName("nashorn");

                CompiledScript compiled = ((Compilable) engine).compile(body);

                compiled.eval();

                return compiled.getEngine();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        return engines.get(key);
    }

    private PropertyResult getEntry(Map<String, Object> item) {
        var command = item.entrySet().stream()
                .filter(entry -> !entry.getKey().equals("target"))
                .findFirst()
                .orElseThrow(IllegalStateException::new);

        String target = item.get("target").toString();

        return new PropertyResult(target, command.getKey(), command.getValue());
    }

    private PropertyValue prepareResults(Map<String, Object> item, InvocationContext invocationContext) {

        var entry = getEntry(item);

        // TODO: check isolation
        return new PropertyValue(
                requireNonNull(invocationContext.getSpace()),
                requireNonNull(invocationContext.getSelf()),
                requireNonNull(entry.getKey()),
                requireNonNull(entry.getTarget()),
                Value.Companion.of(entry.getValue())
        );
    }

    @Transactional(readOnly = true, noRollbackFor = {RuntimeException.class, Error.class})
    public InvocationResult invokeContract(
            PropertyContext propertyContext, InvocationContext invocationContext, List<Value> params) {
        return new InvocationResult(SUCCESS, invokeFunction(propertyContext, invocationContext, params));
    }

    @SuppressWarnings("unchecked")
    private List<PropertyValue> invokeFunction(PropertyContext propertyContext, InvocationContext invocationContext, List<Value> params) {

        try {

            var args = Lists.newArrayList(
                    invocationContext, invocationContext.getCaller(), invocationContext.getCallee());

            var paramValues = params.stream().map(Value::toValue).collect(Collectors.toList());
            args.addAll(paramValues);

            var engine = getEngine(propertyContext, invocationContext);

            var results = (Map<String, Object>) engine.invokeFunction(invocationContext.getKey(), args.toArray());

            return results.values().stream()
                    .map(item -> prepareResults((Map<String, Object>) item, invocationContext))
                    .collect(toList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This is used primarily to invoke `init` function
     */
    @SuppressWarnings("unchecked")
    public List<PropertyResult> invokeFunction(String name, String body) {
        try {

            var engine = (Invocable) getEngine(body);

            var results = (Map<String, Object>) engine.invokeFunction(name);

            return results.values().stream()
                    .map(item -> getEntry((Map<String, Object>) item))
                    .collect(toList());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
