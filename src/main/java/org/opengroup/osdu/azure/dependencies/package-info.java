//  Copyright © Microsoft Corporation
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

/**
 * Contains configuration methods for Azure dependencies.
 *
 * To use this package, you will need to provide any service specific
 * configuration that is required by the dependencies that you need to
 * instantiate. The specific configuration required by each dependency
 * can be found in
 * {@link org.opengroup.osdu.azure.dependencies.AzureConfig}.
 *
 * After doing this, you can then inject the dependency using
 * <pre>@Inject</pre> or <pre>@Autowire</pre>. For example, here
 * is how you would configure KeyVault:
 *
 *
 * Provide a configuration value:
 * <pre>
 * {@code
 *   @Bean
 *   @Named("KEY_VAULT_URL")
 *   public String keyVaultURL() {
 *       return "...";
 *   }
 * }
 * </pre>
 *
 * Inject dependency:
 * <pre>
 * {@code
 *   @Inject
 *   private SecretClient secretClient;
 * }
 * </pre>
 *
 * Don't forget to wire up the configuration in your application
 * entry point:
 * <pre>
 * {@code
 *   public static void main(String[] args) {
 *       Class<?>[] sources = new Class<?>[]{
 *           ...,
 *           AzureConfig.class
 *       };
 *       SpringApplication.run(sources, args);
 *   }
 * }
 * </pre>
 */
package org.opengroup.osdu.azure.dependencies;
