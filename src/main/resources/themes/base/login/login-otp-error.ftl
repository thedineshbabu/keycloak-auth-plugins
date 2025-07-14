<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        ${msg("errorTitle")}
    <#elseif section = "form">
        <div id="kc-form">
            <div id="kc-form-wrapper">
                <div class="alert alert-danger">
                    <span class="pficon pficon-error-circle-o"></span>
                    <span class="kc-feedback-text">
                        <#if errorTitle??>
                            <strong>${errorTitle}</strong><br>
                        </#if>
                        <#if errorMessage??>
                            ${errorMessage}
                        <#else>
                            ${msg("otpErrorGeneric")}
                        </#if>
                    </span>
                </div>

                <div class="form-group">
                    <div class="col-sm-12">
                        <div class="text-center">
                            <a href="${url.loginRestartFlowUrl}" class="btn btn-primary">
                                ${msg("tryAgain")}
                            </a>
                        </div>
                    </div>
                </div>

                <div class="form-group">
                    <div class="col-sm-12">
                        <div class="text-center">
                            <a href="${url.loginUrl}" class="btn btn-link">
                                ${msg("backToLogin")}
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </#if>
</@layout.registrationLayout> 