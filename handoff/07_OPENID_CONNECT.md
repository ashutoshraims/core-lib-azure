# OpenID Connect & OSDU R2

The content in this document is intended to provide an introduction to OpenID Connect and to provide context into how it is used across the OSDU R2 platform.

In scope topics include:

- OpenID Connect
- OAuth
- OpenID Connect usage in OSDU R2 services

Out of scope topics include:

- Technical specifications for OpenID Connect and OAuth
- Detailed discussion about *why* OpenID Connect is used


**Note**: If you are not already well versed with authentication and authorization standards, you may need to re-read this document (as well as the referenced documentation) a few times in order to fully understand the subtle details that this document attempts to describe. The following resources provide a deeper dive into the concepts covered here:

- [OAuth 2.0 and OpenID Connect protocols on the Microsoft identity platform](https://docs.microsoft.com/en-us/azure/active-directory/develop/active-directory-v2-protocols)
- [OAuth Homepage](https://oauth.net/2/)
- [OAuth vs OpenID](http://softwareas.com/oauth-openid-youre-barking-up-the-wrong-tree-if-you-think-theyre-the-same-thing/)

## Authentication vs Authorization

> **Note**: More information on these topics can be found [here](https://docs.microsoft.com/en-us/azure/active-directory/develop/authentication-scenarios) and [here](https://www.okta.com/security-blog/2019/02/the-ultimate-authentication-playbook/)

**Authentication**

Authentication is the act of validating that users are who they claim to be. Examples of authentication methods include *Single Factor Authentication*, *2nd Factor Authentication* and *Multi-Factor Authentication*. 

**Authorization**

Authorization is the process of giving the user permission to access a specific resource or function. This term is often used interchangeably with access control or client privilege. Authorization can happen with or without authentication.


**Still confused?**

Imagine you are checking into a hotel room. When you provide a method of identification at the front desk, you are *authenticating*. Once you get your keycard and use it to access your room, you are *authorizing*. 


## OAuth & OpenID Connect

> **Note**: More information on these topics can be found [here](http://softwareas.com/oauth-openid-youre-barking-up-the-wrong-tree-if-you-think-theyre-the-same-thing/) and [here](https://docs.microsoft.com/en-us/azure/active-directory/develop/v2-protocols-oidc)

**OAuth (Authorization)**

OAuth is an open standard for access delegation, commonly used as a way for Internet users to grant websites or applications access to their information on other websites but without giving them the passwords. OAuth allows access tokens to be issued to third-party clients by an authorization server, with the approval of the resource owner. The third party then uses the access token to access the protected resources hosted by the resource server

> **Note**: The OAuth protocol does not provide any way to prove the *identity* of the user requesting access to the protected resource.

**OAuth (Authentication & Authorization)**

> Borrowed from the [microsoft documentation](https://docs.microsoft.com/en-us/azure/active-directory/develop/v2-protocols-oidc)

*OpenID Connect is an authentication protocol built on OAuth 2.0 that you can use to securely sign in a user to a web application. When you use the Microsoft identity platform endpoint's implementation of OpenID Connect, you can add sign-in and API access to your web-based apps... OpenID Connect extends the OAuth 2.0 authorization protocol to use as an authentication protocol, so that you can do single sign-on using OAuth. OpenID Connect introduces the concept of an ID token, which is a security token that allows the client to verify the identity of the user. The ID token also gets basic profile information about the user.*

## Use of OpenID Connect in OSDU R2

Services in the OSDU R2 platform require that clients provide a `Authorization` header that includes a `Bearer` token. According to the *OpenID Connect* specification, the token will be a [JSON Web Token (JWT)](https://jwt.io/). This token is used by the services to verify the caller's identity and provide authorization checks on resources such as API endpoints and row level data.

In order for clients to produce a *JWT Token*, they can follow any number of flows outlined in the *OpenID Connect* specification. The flows supported by Microsoft can be found [here](https://docs.microsoft.com/en-us/azure/active-directory/develop/v2-oauth2-implicit-grant-flow).

## Next Steps

If you made it this far, you may be interested in some of the following guides to further your understanding and knowledge:

- [Authorize access to web applications using OpenID Connect and Azure Active Directory](https://docs.microsoft.com/en-us/azure/active-directory/azuread-dev/v1-protocols-openid-connect-code)
- [Microsoft identity platform and OAuth 2.0 authorization code flow](https://docs.microsoft.com/en-us/azure/active-directory/develop/v2-oauth2-auth-code-flow)
- [Token Refresh](https://docs.microsoft.com/en-us/azure/active-directory/develop/v2-oauth2-auth-code-flow#refresh-the-access-token)
- [OpenID Connect Specification](https://openid.net/specs/openid-connect-discovery-1_0.html)
- [OpenID Connect User Endpoint](https://openid.net/specs/openid-connect-core-1_0.html#UserInfo)


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
