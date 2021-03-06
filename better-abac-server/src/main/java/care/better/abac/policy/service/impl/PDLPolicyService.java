package care.better.abac.policy.service.impl;

import care.better.abac.audit.PolicyExecutionAuditor;
import care.better.abac.exception.PolicyNotFoundException;
import care.better.abac.jpa.entity.Policy;
import care.better.abac.jpa.repo.PolicyRepository;
import care.better.abac.policy.antlr.PolicyLexer;
import care.better.abac.policy.antlr.PolicyParser;
import care.better.abac.policy.convert.ConvertingPolicyVisitor;
import care.better.abac.policy.definition.PolicyDefinition;
import care.better.abac.policy.execute.PolicyExecutionContext;
import care.better.abac.policy.execute.PolicyHelper;
import care.better.abac.policy.execute.evaluation.EvaluationExpression;
import care.better.abac.policy.service.PolicyService;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * @author Bostjan Lah
 */

public class PDLPolicyService implements PolicyService {
    private static final Logger log = LogManager.getLogger(PDLPolicyService.class.getName());

    private final ConcurrentMap<String, PolicyDefinition> policies = new ConcurrentHashMap<>();

    private final PolicyHelper policyHelper;
    private final PolicyRepository policyRepository;
    private final PolicyExecutionAuditor policyExecutionAuditor;
    
    public PDLPolicyService(PolicyHelper policyHelper, PolicyRepository policyRepository, PolicyExecutionAuditor policyExecutionAuditor) {
        this.policyHelper = policyHelper;
        this.policyRepository = policyRepository;
        this.policyExecutionAuditor = policyExecutionAuditor;
    }

    @Override
    public EvaluationExpression executeByName(String name, Map<String, Object> ctx) {
        log.debug("Excuting policy: {}.", name);
        return execute(ctx, () -> getPolicyDefinition(name));
    }

    @Override
    public EvaluationExpression queryByName(String name, Map<String, Object> ctx) {
        log.debug("Querying policy: {}.", name);
        return query(ctx, () -> getPolicyDefinition(name));
    }

    @Override
    public void policyUpdated(String name, boolean refresh) {
        policies.remove(name);
        if (refresh)
        {
            getPolicyDefinition(name);
        }
    }

    private PolicyDefinition getPolicyDefinition(String name) {
        return policies.computeIfAbsent(name, x -> {
            Policy policy = policyRepository.findByName(name);
            if (policy == null) {
                throw new PolicyNotFoundException(String.format("Policy '%s' not found!", name));
            }
            PolicyLexer lexer = new PolicyLexer(new ANTLRInputStream(policy.getPolicy()));
            PolicyParser parser = new PolicyParser(new CommonTokenStream(lexer));

            ConvertingPolicyVisitor convertingPolicyVisitor = new ConvertingPolicyVisitor();
            return convertingPolicyVisitor.convert(parser.policy());
        });
    }

    private EvaluationExpression execute(Map<String, Object> ctx, Supplier<PolicyDefinition> policyDefinitionSupplier) {
        return policyDefinitionSupplier.get().evaluate(UUID.randomUUID().toString(), new PolicyExecutionContext(ctx, policyHelper), policyExecutionAuditor);
    }

    private EvaluationExpression query(Map<String, Object> ctx, Supplier<PolicyDefinition> policyDefinitionSupplier) {
        return policyDefinitionSupplier.get().query(UUID.randomUUID().toString(), new PolicyExecutionContext(ctx, policyHelper), policyExecutionAuditor);
    }
}
