# Nexconn ChatUI SDK consumer ProGuard rules

-keep class ai.nexconn.chatui.** {
    public *;
    protected *;
}

-keep class * implements ai.nexconn.chatui.channel.extension.IExtensionModule {
    public <init>();
}

-keepattributes Signature,*Annotation*
