<#macro emailLayout>
<html lang="${locale.language}" dir="${(ltr)?then('ltr','rtl')}">
<body>
    <#nested>
<div style="margin-top:24px;padding-top:16px;border-top:1px solid #dfe7e1;color:#667068;font-size:12px;font-family:Arial,sans-serif">
  DonaTrack · Donaciones que se pueden seguir.
</div>
</body>
</html>
</#macro>
