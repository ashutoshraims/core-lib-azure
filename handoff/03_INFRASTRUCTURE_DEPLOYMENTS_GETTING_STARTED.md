# OSDU R2 - Infrastructure Deployments


This section is intended to provide a closer look into how the infrastructure that hosts the OSDU R2 services in Azure is tested and deployed. The accompanying [challenge](./03_INFRASTRUCTURE_DEPLOYMENTS_CHALLENGE.md) will provide hands on experience with the OSDU R2 infrastructure release pipeline.

The following topics are in scope:
- Overview of infrastructure used by OSDU R2 services
- Introduction to infrastructure pipelines in Azure DevOps
- Introduction to [Terraform](https://www.terraform.io/)

The items in the list below are out of scope for this session:

- Introduction to pipelines in Azure DevOps
- Advanced Terraform usage
- Deep dive on service interactions with infrastructure in Azure
- Deploying terraform templates from your local developer workstation

## Overview of infrastructure used by OSDU R2 Services

> Note: The resource names below are from the production environment as of 4/2/2020. The naming convention of these resources will be covered later in this document. There is an in-depth document that discusses this in more detail [here](https://dev.azure.com/slb-des-ext-collaboration/open-data-ecosystem/_git/infrastructure-templates?path=%2Finfra%2Ftemplates%2Faz-micro-svc-small-elastic-cloud%2FREADME.md&_a=preview)

| Resource Type | Example Resource Name | Used By |
| --- | --- | --- |
| App Service Plan | `ado-prod-jnwijvfg-osdu-r2-sp` | Each of the [6 core OSDU R2 services](./02_SERVICE_OVERVIEW.md) run within an App Service Plan |
| App Service | `ado-prod-jnw-jnwijvfg-au-entitlements` | Each of the [6 core OSDU R2 services](./02_SERVICE_OVERVIEW.md) run on an App Service |
| Application Insights | `ado-prod-jnwijvfg-osdu-r2-ai` | Each of the [6 core OSDU R2 services](./02_SERVICE_OVERVIEW.md) use Application Insights for metrics and logging |
| Azure Cache for Redis | `ado-prod-jnwijvfg-osdu-r2-redis` | Unused. Will be removed as a part of [Story 1587](https://dev.azure.com/slb-des-ext-collaboration/open-data-ecosystem/_backlogs/backlog/open-data-ecosystem%20Team/Stories/?workitem=1587) |
| Cosmos DB Account | `ado-prod-jnw-jnwijvfg-cosmosdb` | `os-legal-azure`, `os-storage-azure`, and `os-entitlements-azure` all use CosmosDB |
| Container Registry | `adoprodjnwjnwijvfgcr` | The single Azure Function (enqueue) relies on the Container Registry to hold its Docker Image |
| Key Vault | `ado-prod-jnw-jnwijvfg-kv` | Each of the OSDU R2 services relies on secrets stored in KeyVault |
| Storage Account | `adoprodjnwjnwijvfgsa` | Storage service, Legal service and the enqueue function all use the Storage Account for storing documents and state. There are two storage accounts - one for OSDU R2 documents and one used by the Azure Functions Runtime |
| Service Bus Namespace | `ado-prod-jnw-jnwijvfgsb` | `os-search`, `os-legal` and `os-indexer-queue-azure` all publish or consume messages from Service Bus |
| Elastic Search | `ado-prod-jnw-jnwijvfg-es` | `os-indexer-azure` and `os-search-azure` use Elasticsearch. Note: this resource does not live within the same Azure subscription as the other resources. It is managed by [Elastic Search Service](https://www.elastic.co/elasticsearch/service) or [Elastic Cloud Enterprise](https://www.elastic.co/ece), and the underlying hosting of this service is not something that matters to the rest of the OSDU R2 services. |

## Introduction to Terraform

The infrastructure for OSDU R2 in Azure is defined using [Terraform](https://www.terraform.io/). Here is a description of Terraform pulled from the [Terraform Intro Docs](https://www.terraform.io/intro/index.html):
> Terraform is a tool for building, changing, and versioning infrastructure safely and efficiently.... Configuration files describe to Terraform the components needed to run a single application or your entire datacenter. Terraform generates an execution plan describing what it will do to reach the desired state, and then executes it to build the described infrastructure. As the configuration changes, Terraform is able to determine what changed and create incremental execution plans which can be applied.

This topic can take hours to cover, so we suggest that you take a look at some good guides online to get started. Here are some good resources you may want to consider:
- [Terraform Introduction](https://www.terraform.io/intro/index.html)
- [Getting Started with Terraform on Azure](https://learn.hashicorp.com/terraform/azure/intro_az)

You may also want to look at the documentation for the [template that deploys the OSDU R2 infrastructure](https://dev.azure.com/slb-des-ext-collaboration/open-data-ecosystem/_git/infrastructure-templates?path=%2Finfra%2Ftemplates%2Faz-micro-svc-small-elastic-cloud%2FREADME.md&_a=preview). 

### Key Concepts in Terraform

The following snippet of Terraform will be used to motivate the discussion points below.
> Note: This snippet is not a complete terraform configuration.

```hcl
locals {
  storage_role_principals = [...] # details omitted
}

module "app_management_service_principal" {
  source          = "../../modules/providers/azure/service-principal"
  create_for_rbac = true
  display_name    = "My App Name"
}

resource "azurerm_role_assignment" "storage_roles" {
  count                = length(local.storage_role_principals)
  role_definition_name = "Storage Blob Data Contributor"
  principal_id         = local.storage_role_principals[count.index]
  scope                = module.storage_account.id
}

output "role_assignment_id" {
  value = azurerm_role_assignment.storage_roles.id
}
```

**Resources**
[Resources](https://www.terraform.io/docs/configuration/resources.html) are the most important element in the Terraform language. Each resource block describes one or more infrastructure objects, such as virtual networks, compute instances, or higher-level components such as DNS records.

**Modules**
A [Module](https://www.terraform.io/docs/configuration/modules.html) is a container for multiple resources that are used together. Modules allow for layers of abstraction to be introduced to the infrastructure definition. In this case, the module definition lives in a folder that sits in the same repository as the snippet shown here. Modules can also reference remote sources.

**Outputs**
[Outputs](https://www.terraform.io/docs/configuration/outputs.html) values are like the return values of a Terraform module. These values can be shared across deployments and can provide useful information using the `terraform show` command.

**Plan**
The terraform [plan](https://www.terraform.io/docs/commands/plan.html) command is a convenient way to check whether the execution plan for a set of changes matches your expectations without making any changes to real resources or to the state. For example, terraform plan might be run before committing a change to version control, to create confidence that it will behave as expected.

**Refresh**
The terraform [refresh](https://www.terraform.io/docs/commands/refresh.html) command is used to reconcile the state Terraform knows about (via its state file) with the real-world infrastructure. This can be used to detect any drift from the last-known state, and to update the state file. This does not modify infrastructure, but does modify the state file. If the state is changed, this may cause changes to occur during the next plan or apply.

**Apply**
The terraform [apply](https://www.terraform.io/docs/commands/apply.html) command is used to apply the changes required to reach the desired state of the configuration, or the pre-determined set of actions generated by a terraform plan execution plan.

**Backends & State**
Terraform manages the state of the environment being managed using [State](https://www.terraform.io/docs/state/index.html). [Backends](https://www.terraform.io/docs/backends/state.html) are a way to persist this state to a remote storage target, which allows for terraform deployments to be managed through CI/CD environments.

> Note: if you make a manual change in the Azure Portal, terraform will find them when the `refresh` command runs, and then undo them when the `apply` command runs. It is **not recommended** to make manual changes to the terraform managed infrastructure.

**Workspace**
A Terraform [workspace](https://www.terraform.io/docs/state/workspaces.html) can be used to create a parallel, distinct copy of a set of infrastructure in order to test a set of changes before modifying the main production infrastructure. For example, a developer working on a complex set of infrastructure changes might create a new temporary workspace in order to freely experiment with changes without affecting the default workspace. In OSDU R2, we use these to isolate our `devint`, `qa` and `production` deployments.


### Resource Naming Conventions

A common scenario with automated infrastructure deployments is that names for resources with public DNS entries may already be taken by another deployment. The templates used for OSDU R2 solve this by leveraging the following two elements to attempt to produce a unique name for each resource:
- Terraform workspace
- Random ID

The logic for constructing unique names often lives within a file called [`commons.tf`](https://dev.azure.com/slb-des-ext-collaboration/open-data-ecosystem/_git/infrastructure-templates?path=%2Finfra%2Ftemplates%2Faz-micro-svc-small-elastic-cloud%2Fcommons.tf).

### Terraform Unit & Integration Testing

The opensource community has aligned on using [Go](https://golang.org/) and [Terratest](https://github.com/gruntwork-io/terratest) to run tests against Terraform templates. Some helpful abstractions have been built over these technologies to make testing simpler and more declarative. The documentation for this testing library can be found [here](https://dev.azure.com/slb-des-ext-collaboration/open-data-ecosystem/_git/infrastructure-templates?path=%2Ftest-harness).

## Introduction to infrastructure pipelines

The infrastructure needed in OSDU R2 is created and managed through release pipelines in Azure DevOps. The [pipeline](https://dev.azure.com/slb-des-ext-collaboration/open-data-ecosystem/_build?definitionId=167&_a=summary) is triggered by commits and pull requests to the [infrastructure-templates](https://dev.azure.com/slb-des-ext-collaboration/open-data-ecosystem/_git/infrastructure-templates) repository hosted in Azure DevOps. 

**Note**: Detailed documentation on these pipelines can be found [here](https://dev.azure.com/slb-des-ext-collaboration/open-data-ecosystem/_git/infrastructure-templates?path=%2Fdocs%2Fosdu%2FINFRASTRUCTURE_DEPLOYMENTS.md&_a=preview).

### What does the pipeline do?

In order to accomplish the high level goal of building and releasing infrastructure changes in a controlled manner, the pipeline must orchestrate the following steps. A more detailed breakdown of these steps can be found [here](https://dev.azure.com/slb-des-ext-collaboration/open-data-ecosystem/_git/infrastructure-templates?path=%2Fdocs%2Fosdu%2FINFRASTRUCTURE_DEPLOYMENTS.md&_a=preview), but this document will provide a high level overview as well.

**Prepare stage** 
> The prepare stage is executed only one time

This stage will ensure that the codebase passes basic lint checks. The primary use for this stage is to provide quick feedback to developers about code formatting and style. The stage orchestrates the following tasks:
- Validate that the Terraform code passes lint checks
- Validate that the Testing code (written in Go) passes lint checks. We will cover Unit Testing with Terraform later in this document.
- Package an artifact containing the infrastructure template that can be consumed by subsequent stages

**`$STAGE` build stage**
> The build stage is executed once per target environment (`devint`, `qa`, `prod`, etc...)

This stage will ensure that the codebase passes basic validation and unit testing checks. The primary use for this stage is to provide quick feedback to developers about any errors that can be checked before the deployment. The stage orchestrates the following tasks:
- Validate that the Terraform contains a valid configuration
- Validate that the Terraform code passes unit tests
- Execute a [Terraform Plan](https://www.terraform.io/docs/commands/plan.html) (more on this later), which is essentially a declaration about what Terraform will change if a release is executed.
- Package an artifact containing the resulting `plan` that can be consumed by the subsequent release stage

**`$STAGE` release stage**
> The release stage is executed once per target environment (`devint`, `qa`, `prod`, etc...)

This stage will ensure that proposed change from the `build` stage is able to be applied, and that the resulting infrastructure passes integration tests. The primary use for this stage is to deploy changes and validate those changes using automated tests. The stage orchestrates the following tasks:
- Apply the proposed changes from the `build` stage
- Execute automated integration tests that validate that the deployment was successful. We will cover Integration Testing with Terraform later in this document.

### Controlling releases

A number of steps are taken to make sure that infrastructure changes are released in a controlled manner.

**Branch Policies**

Merges directly into master are not allowed in the `infrastructure-templates` repository. A pull request is required to merge into master. Also, a successful release of the pipeline is enforced as a branch policy. 

![Branch Policy](./.images/03_branch_policy.png)

**Pull request vs master release**

The environments deployed to for pull requests differ from master. This prevents low quality deployments from releasing into customer facing environments.

| Environment | Deployed to for Pull Request? | Deployed to for Master? |
| --- | --- | --- |
| `devint` | yes | yes |
| `qa` | no | yes |
| `prod` | no | yes |

This behavior is controlled by the YAML templates that define the build and release process for the infrastructure. We'll cover this in more detail later, but here is a glimpse into how this is achieved:

```yaml
# Note: a lot of details are omitted, but will be discussed later in this document
stages:
- template: prepare-stage.yml
  parameters:
    environments:
    - name: 'devint'
    - ${{ if eq(variables['Build.SourceBranchName'], 'master') }}:
      - name: 'qa'
      - name: 'prod'
```

**Release Gates**

Release gates can be used to mandate that certain criteria are met before a release will be executed into a particular stage. Information on how to configure these can be found [here](https://docs.microsoft.com/en-us/azure/devops/pipelines/process/approvals?view=azure-devops&tabs=check-pass).

### Usage of YAML

The release pipeline for the infrastructure template is defined as a [YAML pipeline](https://docs.microsoft.com/en-us/azure/devops/pipelines/yaml-schema). This allows for the pipeline definition to be controlled in source code. 

**Templating**
One of the biggest advantages of using a YAML based pipeline the [templating capability](https://docs.microsoft.com/en-us/azure/devops/pipelines/process/templates?view=azure-devops). This allows for the definition of steps like **build** and **release** to be standardized and configurable by using [parameters](https://docs.microsoft.com/en-us/azure/devops/pipelines/process/templates?view=azure-devops#parameters).

**Variable Scoping**
Using YAML pipelines it is possible to have control over which variables are in scope for which stages/jobs/tasks. It is also possible to override variables when needed. More details on this can be found [here](https://docs.microsoft.com/en-us/azure/devops/pipelines/process/variables?view=azure-devops&tabs=yaml%2Cbatch#variable-scopes).

**Trigger, Steps, Jobs, Tasks**
It is important to understand `Triggers`, `Steps`, `Jobs`, and `Tasks` well when looking at the pipelines. At a high level:
- A trigger tells a Pipeline to run.
- A pipeline is made up of one or more stages. A pipeline can deploy to one or more environments.
- A stage is a way of organizing jobs in a pipeline and each stage can have one or more jobs.
- A step can be a task or script and is the smallest building block of a pipeline.
- A task is a pre-packaged script that performs an action, such as invoking a REST API or publishing a build artifact.

More details on this can be found [here](https://docs.microsoft.com/en-us/azure/devops/pipelines/get-started/key-pipelines-concepts?view=azure-devops).

## Next Steps

Now that you have gotten this far, you may want to try to deploy the infrastructure into a new stage by following the [challenge](./03_INFRASTRUCTURE_DEPLOYMENTS_CHALLENGE.md) document!

## License
Copyright © Microsoft Corporation
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
