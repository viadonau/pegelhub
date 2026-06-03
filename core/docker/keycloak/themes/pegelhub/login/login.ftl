<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=(realm.password && realm.registrationAllowed && !registrationDisabled??); section>

    <#if section = "header">
        ${msg("doLogIn")}

    <#elseif section = "form">
        <#if realm.password>
            <form id="kc-form-login" class="ph-login-form" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
                <#if messagesPerField.existsError('username','password')>
                    <div class="ph-alert ph-alert--error" role="alert">
                        ${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}
                    </div>
                </#if>

                <#if !usernameHidden??>
                    <div class="ph-field">
                        <label for="username" class="ph-field-label">
                            <#if !realm.loginWithEmailAllowed>${msg("username")}
                            <#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}
                            <#else>${msg("email")}</#if>
                        </label>
                        <span class="ph-input-shell">
                            <input
                                tabindex="1"
                                id="username"
                                class="ph-input"
                                name="username"
                                value="${(login.username!'')}"
                                type="text"
                                autofocus
                                autocomplete="username"
                                aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
                            />
                        </span>
                    </div>
                </#if>

                <div class="ph-field">
                    <label for="password" class="ph-field-label">${msg("password")}</label>
                    <span class="ph-input-shell">
                        <input
                            tabindex="2"
                            id="password"
                            class="ph-input"
                            name="password"
                            type="password"
                            autocomplete="current-password"
                            aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
                        />
                    </span>
                </div>

                <div class="ph-row-between">
                    <#if realm.rememberMe && !usernameHidden??>
                        <label class="ph-checkbox">
                            <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox" <#if login.rememberMe??>checked</#if>/>
                            <span>${msg("rememberMe")}</span>
                        </label>
                    <#else>
                        <span></span>
                    </#if>

                    <#if realm.resetPasswordAllowed>
                        <a tabindex="5" href="${url.loginResetCredentialsUrl}" class="ph-link">${msg("doForgotPassword")}</a>
                    </#if>
                </div>

                <input type="hidden" id="id-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>

                <button tabindex="4" class="ph-button" name="login" id="kc-login" type="submit">${msg("doLogIn")}</button>
            </form>
        </#if>

    <#elseif section = "info">
        <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
            <span>${msg("noAccount")} <a tabindex="6" href="${url.registrationUrl}" class="ph-link">${msg("doRegister")}</a></span>
        </#if>

    <#elseif section = "socialProviders">
        <#if realm.password && social?? && social.providers?has_content>
            <div class="ph-divider" role="presentation"></div>
            <ul class="ph-social-list">
                <#list social.providers as p>
                    <li>
                        <a id="social-${p.alias}" class="ph-social-link" href="${p.loginUrl}">
                            <#if p.iconClasses?has_content><i class="${p.iconClasses!}" aria-hidden="true"></i></#if>
                            <span>${p.displayName!}</span>
                        </a>
                    </li>
                </#list>
            </ul>
        </#if>
    </#if>

</@layout.registrationLayout>
