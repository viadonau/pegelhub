<#import "template.ftl" as layout>
<#assign minimumPasswordLength = (passwordPolicies.length)!12>
<#assign hasFieldPasswordError = messagesPerField.existsError('password','password-confirm')>
<#assign hasGlobalPasswordError = !hasFieldPasswordError && message?has_content && message.type == 'error'>

<@layout.registrationLayout displayMessage=!hasFieldPasswordError; section>
    <#if section = "header">
        ${msg("updatePasswordTitle")}

    <#elseif section = "form">
        <form id="kc-passwd-update-form" class="ph-login-form" action="${url.loginAction}" method="post">
            <div class="ph-field">
                <label for="password-new" class="ph-field-label">${msg("passwordNew")}</label>
                <span class="ph-input-shell">
                    <input
                        id="password-new"
                        class="ph-input"
                        name="password-new"
                        type="password"
                        minlength="${minimumPasswordLength}"
                        required
                        autofocus
                        autocomplete="new-password"
                        aria-describedby="password-requirements<#if messagesPerField.existsError('password')> input-error-password<#elseif hasGlobalPasswordError> ph-global-message</#if>"
                        aria-invalid="<#if messagesPerField.existsError('password') || hasGlobalPasswordError>true<#else>false</#if>"
                    />
                </span>
                <p id="password-requirements" class="ph-field-help">
                    ${msg("pegelhubPasswordRequirements", minimumPasswordLength)}
                </p>
                <#if messagesPerField.existsError('password')>
                    <p id="input-error-password" class="ph-field-error" role="alert">
                        ${kcSanitize(messagesPerField.get('password'))?no_esc}
                    </p>
                </#if>
            </div>

            <div class="ph-field">
                <label for="password-confirm" class="ph-field-label">${msg("passwordConfirm")}</label>
                <span class="ph-input-shell">
                    <input
                        id="password-confirm"
                        class="ph-input"
                        name="password-confirm"
                        type="password"
                        minlength="${minimumPasswordLength}"
                        required
                        autocomplete="new-password"
                        <#if messagesPerField.existsError('password-confirm')>aria-describedby="input-error-password-confirm"</#if>
                        aria-invalid="<#if messagesPerField.existsError('password-confirm')>true<#else>false</#if>"
                    />
                </span>
                <#if messagesPerField.existsError('password-confirm')>
                    <p id="input-error-password-confirm" class="ph-field-error" role="alert">
                        ${kcSanitize(messagesPerField.get('password-confirm'))?no_esc}
                    </p>
                </#if>
            </div>

            <label class="ph-checkbox ph-session-option">
                <input type="checkbox" id="logout-sessions" name="logout-sessions" value="on" checked />
                <span>${msg("logoutOtherSessions")}</span>
            </label>

            <div class="ph-button-stack">
                <button class="ph-button" type="submit">${msg("doSubmit")}</button>
                <#if isAppInitiatedAction??>
                    <button class="ph-button ph-button--secondary" type="submit" name="cancel-aia" value="true">
                        ${msg("doCancel")}
                    </button>
                </#if>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
