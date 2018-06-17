package org.bloqly.machine.service;

import com.google.common.collect.Lists;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.bloqly.machine.function.GetPropertyFunction;
import org.bloqly.machine.model.InvocationContext;
import org.bloqly.machine.model.Property;
import org.bloqly.machine.model.PropertyId;
import org.bloqly.machine.repository.ContractRepository;
import org.bloqly.machine.repository.PropertyRepository;
import org.bloqly.machine.repository.PropertyService;
import org.bloqly.machine.util.ParameterUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.script.Invocable;
import javax.script.ScriptEngineManager;
import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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

    private List<Property> invokeFunction(InvocationContext context, byte[] arg) {

        try {

            var contract = context.getContract();

            var params = ParameterUtils.INSTANCE.readParams(arg);

            List<Object> args = Lists.newArrayList(context, context.getCaller(), context.getCallee());

            args.addAll(Arrays.asList(params));

            System.setProperty("nashorn.args", "--language=es6");

            var engine = new ScriptEngineManager().getEngineByName("nashorn");

            engine.put("getProperty", getPropertyFunction(context));

            engine.eval(contract.getBody());

            var invocable = (Invocable) engine;

            var results = (ScriptObjectMirror) invocable.invokeFunction(context.getFunctionName(), args.toArray());

            return results.values().stream()
                    .map(item -> prepareResults((ScriptObjectMirror) item, context))
                    .collect(toList());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Property prepareResults(ScriptObjectMirror item, InvocationContext context) {

        var command = item.entrySet().stream()
                .filter(entry -> !entry.getKey().equals("target"))
                .findFirst()
                .orElseThrow(IllegalStateException::new);

        String target = item.get("target").toString();

        var contract = Objects.requireNonNull(context.getContract());
        // TODO: check isolation
        return new Property(
                new PropertyId(
                        Objects.requireNonNull(contract.getSpace()),
                        Objects.requireNonNull(contract.getId()),
                        Objects.requireNonNull(target),
                        command.getKey()
                ),
                ParameterUtils.INSTANCE.writeValue(command.getValue())
        );
    }

    @Transactional
    public void invokeContract(String functionName, String self, String caller, String callee, byte[] arg) {

        contractRepository.findById(self).ifPresent(contract -> {

            var invocationContext = new InvocationContext(functionName, caller, callee, contract);

            var properties = invokeFunction(invocationContext, arg);

            propertyService.updateProperties(properties);
        });
    }

}
