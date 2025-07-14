<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('otp'); section>
    <#if section = "header">
        ${msg("doSubmit")}
    <#elseif section = "form">
        <div id="kc-form">
            <div id="kc-form-wrapper">
                <#if message?has_content && (message.type == 'warning' || message.type == 'error')>
                    <div class="alert alert-${message.type}">
                        <#if message.type == 'warning'>
                            <span class="pficon pficon-warning-triangle-o"></span>
                        </#if>
                        <#if message.type == 'error'>
                            <span class="pficon pficon-error-circle-o"></span>
                        </#if>
                        <span class="kc-feedback-text">${kcSanitize(message.summary)?no_esc}</span>
                    </div>
                </#if>

                <#if formData?? && formData.message??>
                    <div class="alert alert-info">
                        <span class="pficon pficon-info"></span>
                        <span class="kc-feedback-text">${formData.message}</span>
                    </div>
                </#if>

                <form id="kc-otp-form" class="form-horizontal" action="${url.loginAction}" method="post">
                    <div class="form-group">
                        <div class="col-sm-12">
                            <div class="input-group">
                                <span class="input-group-addon">
                                    <i class="fa fa-key"></i>
                                </span>
                                <input type="text" 
                                       class="form-control" 
                                       id="otp" 
                                       name="otp" 
                                       value="${(form.otp!'')}"
                                       autocomplete="off"
                                       placeholder="${msg("otpPlaceholder")}"
                                       maxlength="6"
                                       pattern="[0-9]{6}"
                                       required
                                       autofocus>
                            </div>
                        </div>
                    </div>

                    <div class="form-group">
                        <div class="col-sm-12">
                            <div id="kc-form-buttons" class="form-group">
                                <input class="btn btn-primary btn-block btn-lg" 
                                       name="login" 
                                       id="kc-login" 
                                       type="submit" 
                                       value="${msg("doSubmit")}"/>
                            </div>
                        </div>
                    </div>

                    <div class="form-group">
                        <div class="col-sm-12">
                            <div class="text-center">
                                <button type="submit" 
                                        name="resend_otp" 
                                        value="true" 
                                        class="btn btn-link">
                                    ${msg("resendOtp")}
                                </button>
                            </div>
                        </div>
                    </div>

                    <#if formData?? && formData.email??>
                        <div class="form-group">
                            <div class="col-sm-12">
                                <div class="text-center">
                                    <small class="text-muted">
                                        ${msg("otpSentTo")} <strong>${formData.email}</strong>
                                    </small>
                                </div>
                            </div>
                        </div>
                    </#if>

                    <div class="form-group">
                        <div class="col-sm-12">
                            <div class="text-center">
                                <small class="text-muted">
                                    ${msg("otpExpiresIn")} <span id="otp-timer">5:00</span>
                                </small>
                            </div>
                        </div>
                    </div>
                </form>
            </div>
        </div>

        <script>
            // OTP timer countdown
            let timeLeft = 300; // 5 minutes in seconds
            const timerElement = document.getElementById('otp-timer');
            
            function updateTimer() {
                const minutes = Math.floor(timeLeft / 60);
                const seconds = timeLeft % 60;
                timerElement.textContent = `${minutes}:${seconds.toString().padStart(2, '0')}`;
                
                if (timeLeft <= 0) {
                    timerElement.textContent = '0:00';
                    timerElement.style.color = 'red';
                    return;
                }
                
                timeLeft--;
                setTimeout(updateTimer, 1000);
            }
            
            // Start timer when page loads
            updateTimer();
            
            // Auto-submit form when OTP is complete
            document.getElementById('otp').addEventListener('input', function(e) {
                if (e.target.value.length === 6) {
                    document.getElementById('kc-otp-form').submit();
                }
            });
        </script>
    </#if>
</@layout.registrationLayout> 