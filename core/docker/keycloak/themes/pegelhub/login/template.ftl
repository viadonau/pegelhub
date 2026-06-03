<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false displayWide=false>
<!DOCTYPE html>
<html class="${properties.kcHtmlClass!}" lang="${(locale.currentLanguageTag)!'en'}">

<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">
    <meta name="robots" content="noindex, nofollow">
    <title>${msg("loginTitle",(realm.displayName!''))} · PegelHub</title>
    <link rel="icon" href="${url.resourcesPath}/img/pegelhub-logo.png" />
    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${url.resourcesPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
</head>

<body class="ph-login-body ${bodyClass}">
    <header class="ph-login-header" aria-label="PegelHub">
        <div class="ph-login-header-inner">
            <div class="ph-login-brand">
                <img src="${url.resourcesPath}/img/pegelhub-logo.png" alt="" class="ph-login-brand-logo" />
                <span class="ph-login-brand-wordmark">
                    <span class="ph-login-brand-org">viadonau</span>
                </span>
            </div>
        </div>
    </header>

    <main class="ph-login-page">
        <section class="ph-login-panel" aria-labelledby="ph-login-title">
            <div class="ph-login-heading">
                <h1 class="ph-login-title" id="ph-login-title">
                    <#nested "header">
                </h1>
            </div>

            <#-- Server-side messages (errors / warnings / info) -->
            <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                <div class="ph-alert ph-alert--${message.type}" role="alert">
                    <span>${kcSanitize(message.summary)?no_esc}</span>
                </div>
            </#if>

            <#nested "form">

            <#if displayInfo>
                <div class="ph-divider" role="presentation"></div>
                <div class="ph-footer-note">
                    <#nested "info">
                </div>
            </#if>
        </section>
    </main>
</body>
</html>
</#macro>
