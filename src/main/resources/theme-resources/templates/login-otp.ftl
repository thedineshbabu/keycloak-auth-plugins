<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('otp'); section>
    <#if section = "header">
        ${msg("doLogIn")}
    <#elseif section = "form">
        <div id="kc-form">
            <div id="kc-form-wrapper">
                <#if message?? && message?has_content>
                    <div class="alert alert-${message.type!'info'}">
                        <span class="kc-feedback-text">${message.summary!''}</span>
                    </div>
                </#if>

                <form id="kc-otp-login-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
                    <div class="${properties.kcFormGroupClass!}">
                        <div class="${properties.kcLabelWrapperClass!}">
                            <label for="otp" class="${properties.kcLabelClass!}">OTP Code</label>
                        </div>
                        <div class="${properties.kcInputWrapperClass!}">
                            <input type="text" id="otp" name="otp" class="${properties.kcInputClass!}" autofocus autocomplete="one-time-code" placeholder="Enter 6-digit code" maxlength="6" />
                        </div>
                    </div>

                    <#if email?? && email?has_content>
                    <div class="${properties.kcFormGroupClass!}">
                        <div class="${properties.kcLabelWrapperClass!}">
                            <label class="${properties.kcLabelClass!}">Email</label>
                        </div>
                        <div class="${properties.kcInputWrapperClass!}">
                            <span class="${properties.kcInputClass!}">${email}</span>
                        </div>
                    </div>
                    </#if>

                    <div class="${properties.kcFormGroupClass!}">
                        <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                            <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" 
                                   name="login" id="kc-login" type="submit" value="Verify OTP"/>
                        </div>
                    </div>

                    <div class="${properties.kcFormGroupClass!}">
                        <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                            <div class="${properties.kcFormOptionsWrapperClass!}">
                                <span><a href="${url.loginUrl}?resend_otp=true">Resend OTP</a></span>
                            </div>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </#if>
</@layout.registrationLayout> 