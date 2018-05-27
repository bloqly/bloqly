package org.bloqly.machine.service;

import com.google.common.collect.Lists;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.apache.commons.lang3.StringUtils;
import org.bloqly.machine.function.GetPropertyFunction;
import org.bloqly.machine.model.Contract;
import org.bloqly.machine.model.Genesis;
import org.bloqly.machine.model.Property;
import org.bloqly.machine.model.PropertyId;
import org.bloqly.machine.repository.ContractRepository;
import org.bloqly.machine.repository.PropertyRepository;
import org.bloqly.machine.util.ParameterUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.script.Invocable;
import javax.script.ScriptEngineManager;
import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Service
public class ContractService {

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    private GetPropertyFunction getPropertyFunction(ContractInvocationContext context) {

        return (targetString, key, defaultValue) -> {

            String target = getTarget(targetString, context);

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

    private Set<Property> invokeFunction(ContractInvocationContext context, Genesis genesis, byte[] arg) {

        try {

            System.setProperty("nashorn.args", "--language=es6");

            var contract = context.getContract();

            var engine = new ScriptEngineManager().getEngineByName("nashorn");

            engine.put("getProperty",
                    getPropertyFunction(context));

            engine.eval(contract.getBody());

            var invocable = (Invocable) engine;

            var params = ParameterUtils.INSTANCE.readParams(arg);

            List<Object> args = Lists.newArrayList(context);

            if (genesis != null) {
                args.add(genesis);
            }

            args.addAll(Arrays.asList(params));

            var results = (ScriptObjectMirror) invocable.invokeFunction(context.getFunctionName(), args.toArray());

            return results.values().stream()
                    .map(item -> prepareResults((ScriptObjectMirror) item, context))
                    .collect(toSet());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Property prepareResults(ScriptObjectMirror item, ContractInvocationContext context) {

        var command = item.entrySet().stream()
                .filter(entry -> !entry.getKey().equals("target"))
                .findFirst()
                .orElseThrow(IllegalStateException::new);

        String target = getTarget(item.get("target").toString(), context);

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

    private String getTarget(String targetString, ContractInvocationContext context) {

        String target;

        switch (targetString) {

            case "self":
                target = context.getContract().getId();
                break;

            case "owner":
                target = context.getContract().getOwner();
                break;

            case "caller":
                target = context.getCaller();
                break;

            case "callee":
                target = context.getCallee();
                break;

            default:
                target = targetString;
        }
        return target;
    }

    @Transactional
    public void createContract(String space,
                               String self,
                               Genesis genesis,
                               String body) {

        if (StringUtils.isEmpty(body)) {
            throw new IllegalArgumentException("Contract body can not be empty");
        }

        var contract = new Contract(
                self,
                space,
                genesis.getRoot().getId(),
                body
        );

        var invocationContext = new ContractInvocationContext("init", genesis.getRoot().getId(), self, contract);

        var properties = invokeFunction(invocationContext, genesis, new byte[0]);
        processResults(properties);

        contractRepository.save(contract);
    }

    @Transactional
    public void invokeContract(String functionName, String self, String caller, String callee, byte[] arg) {

        contractRepository.findById(self).ifPresent(contract -> {

            var invocationContext = new ContractInvocationContext(functionName, caller, callee, contract);

            var properties = invokeFunction(invocationContext, null, arg);
            processResults(properties);

        });
    }

    private void processResults(Set<Property> properties) {

        properties.forEach(property -> propertyRepository.save(property));
    }

    @Transactional
    public Contract findById(String self) {

        return contractRepository.findById(self).orElseThrow(RuntimeException::new);
    }
}
