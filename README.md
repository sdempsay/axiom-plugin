# axiom-plugin

GitHub: [sdempsay/axiom-plugin](https://github.com/sdempsay/axiom-plugin)

Submodule of the [Axiom umbrella](https://github.com/sdempsay/axiom). Sibling: [axiom-mcp](https://github.com/sdempsay/axiom-mcp).

## Job

Schema, Java model, `@AgentCapability` annotations, and the Maven plugin that attaches `classifier=agent-catalog`.

Libraries depend on this plugin. They do not depend on `axiom-mcp`.

## Coordinates

- groupId `org.dempsay.axiom`
- artifacts: `axiom-model`, `axiom-annotations`, `axiom-maven-plugin`
- parent `org.dempsay.maven:dempsay-parent`
- Java 21
- packages `org.dempsay.axiom.model`, `.annotations`, `.plugin`

## Layout

```text
axiom-plugin/
├── schema/
│   ├── axiom-catalog-1.json
│   └── examples/
├── model/          # org.dempsay.axiom:axiom-model
├── plugin/         # org.dempsay.axiom:axiom-maven-plugin
└── annotations/    # org.dempsay.axiom:axiom-annotations
```

```bash
mvn -DskipDocker install
```

PRDs (in the umbrella): [C1](https://github.com/sdempsay/axiom/blob/master/prds/C1-catalog-schema.md), [C2](https://github.com/sdempsay/axiom/blob/master/prds/C2-maven-plugin.md).
