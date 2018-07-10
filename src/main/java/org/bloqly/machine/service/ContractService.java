package org.bloqly.machine.service;

import com.google.common.collect.Lists;
import org.bloqly.machine.function.GetPropertyFunction;
import org.bloqly.machine.model.*;
import org.bloqly.machine.repository.ContractRepository;
import org.bloqly.machine.repository.PropertyRepository;
import org.bloqly.machine.repository.PropertyService;
import org.bloqly.machine.util.ObjectUtils;
import org.bloqly.machine.util.ParameterUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.script.Invocable;
import javax.script.ScriptEngineManager;
import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

@Service
public class ContractService {

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PropertyService propertyService;

    private GetPropertyFunction getPropertyFunction(InvocationContext context) {

        return (target, key, defaultValue) -> {

            var propertyKey = new PropertyId(
                    context.getContract().getSpace(),
                    context.getContract().getId(),
                    target,
                    key
            );

            var propertyOpt = propertyRepository.findById(propertyKey);

            if (propertyOpt.isPresent()) {
                Property property = propertyOpt.get();

                return ParameterUtils.INSTANCE.readValue(property.getValue());
            } else {
                return defaultValue;
            }
        };
    }

    private Invocable getEngine(InvocationContext context) throws Exception {
        System.setProperty("nashorn.args", "--language=es6");

        var engine = new ScriptEngineManager().getEngineByName("nashorn");

        engine.put("getProperty", getPropertyFunction(context));

        engine.eval(context.getContract().getBody());

        return (Invocable) engine;
    }

    private Invocable getEngine(String body) throws Exception {
        System.setProperty("nashorn.args", "--language=es6");

        var engine = new ScriptEngineManager().getEngineByName("nashorn");

        engine.eval(body);

        return (Invocable) engine;
    }


    @SuppressWarnings("unchecked")
    public List<PropertyResult> invokeFunction(String name, String body) {
        try {

            var engine = getEngine(body);

            var results = (Map<String, Object>) engine.invokeFunction(name);

            return results.values().stream()
                    .map(item -> getEntry((Map<String, Object>) item))
                    .collect(toList());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Property> invokeFunction(InvocationContext context, byte[] arg) {

        try {

            var params = ParameterUtils.INSTANCE.readParams(arg);

            List<Object> args = Lists.newArrayList(context, context.getCaller(), context.getCallee());

            args.addAll(Arrays.asList(params));

            var engine = getEngine(context);

            var results = (Map<String, Object>) engine.invokeFunction(context.getFunctionName(), args.toArray());

            return results.values().stream()
                    .map(item -> prepareResults((Map<String, Object>) item, context))
                    .collect(toList());

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

    private Property prepareResults(Map<String, Object> item, InvocationContext context) {

        var entry = getEntry(item);

        var contract = requireNonNull(context.getContract());
        // TODO: check isolation
        return new Property(
                new PropertyId(
                        requireNonNull(contract.getSpace()),
                        requireNonNull(contract.getId()),
                        requireNonNull(entry.getTarget()),
                        requireNonNull(entry.getKey())
                ),
                ParameterUtils.INSTANCE.writeValue(entry.getValue())
        );
    }

    @Transactional
    public InvocationResult invokeContract(String functionName, String self, String orig, String dest, byte[] arg) {

        return contractRepository.findById(self).map(contract -> {

            var invocationContext = new InvocationContext(functionName, orig, dest, contract);

            var properties = invokeFunction(invocationContext, arg);

            propertyService.updateProperties(properties);

            var resultData = ObjectUtils.INSTANCE.writeValueAsString(properties);

            return new InvocationResult(InvocationResultType.SUCCESS, resultData);
        }).orElseThrow();
    }

}
