package org.bloqly.machine.service;

import com.google.common.collect.Lists;
import org.bloqly.machine.component.PropertyContext;
import org.bloqly.machine.function.GetPropertyFunction;
import org.bloqly.machine.model.*;
import org.bloqly.machine.util.ParameterUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.script.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.bloqly.machine.model.InvocationResultType.SUCCESS;

@Service
public class ContractExecutorService {

    private GetPropertyFunction getPropertyFunction(PropertyContext propertyContext, InvocationContext invocationContext) {

        return (target, key, defaultValue) -> {

            var propertyValue = propertyContext.getPropertyValue(
                    invocationContext.getSpace(),
                    invocationContext.getSelf(),
                    target,
                    key
            );

            if (propertyValue != null) {
                return ParameterUtils.INSTANCE.readValue(propertyValue);
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

        try {
            System.setProperty("nashorn.args", "--language=es6");

            var engine = new ScriptEngineManager().getEngineByName("nashorn");

            CompiledScript compiled = ((Compilable) engine).compile(body);

            compiled.eval();

            return compiled.getEngine();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PropertyResult getEntry(Map<String, Object> item) {
        var command = item.entrySet().stream()
                .filter(entry -> !entry.getKey().equals("target"))
                .findFirst()
                .orElseThrow(IllegalStateException::new);

        String target = item.get("target").toString();

        return new PropertyResult(target, command.getKey(), command.getValue());
    }

    private Property prepareResults(Map<String, Object> item, InvocationContext invocationContext) {

        var entry = getEntry(item);

        // TODO: check isolation
        return new Property(
                new PropertyId(
                        requireNonNull(invocationContext.getSpace()),
                        requireNonNull(invocationContext.getSelf()),
                        requireNonNull(entry.getTarget()),
                        requireNonNull(entry.getKey())
                ),
                ParameterUtils.INSTANCE.writeValue(entry.getValue())
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public InvocationResult invokeContract(PropertyContext propertyContext, InvocationContext invocationContext, byte[] arg) {
        var properties = invokeFunction(propertyContext, invocationContext, arg);

        return new InvocationResult(SUCCESS, properties);
    }

    @SuppressWarnings("unchecked")
    private List<Property> invokeFunction(PropertyContext propertyContext, InvocationContext invocationContext, byte[] arg) {

        try {

            var params = ParameterUtils.INSTANCE.readParams(arg);

            List<Object> args = Lists.newArrayList(
                    invocationContext, invocationContext.getCaller(), invocationContext.getCallee());

            args.addAll(Arrays.asList(params));

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
