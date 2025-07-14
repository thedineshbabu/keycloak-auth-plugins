<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Sign in to your account</title>
    <link rel="stylesheet" href="../resources/css/login.css">
</head>
<body>
    <div class="kf-center-wrap">
        <div class="kf-card">
            <div class="kf-logo">KORN FERRY</div>
            <div class="kf-title">TALENT SUITE</div>
            <div class="kf-subtitle">Sign in to your account</div>
            <#if message?has_content>
                <div class="kf-message">${message.summary}</div>
            </#if>
            <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
                <label class="kf-label" for="username">Username or email</label>
                <input id="username" name="username" type="text" value="${username!''}" autofocus autocomplete="username" required class="kf-input">
                <label class="kf-label" for="password">Password</label>
                <input id="password" name="password" type="password" autocomplete="current-password" required class="kf-input">
                <button class="kf-btn" type="submit" id="login">Sign In</button>
            </form>
            <div class="kf-footer">
                &copy; Korn Ferry. All rights reserved.
            </div>
        </div>
    </div>
</body>
</html> 