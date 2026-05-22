package ass.example.core;

public enum WindowMode {
    DEFAULT("menu.settings.window.defaultSize"),
    CUSTOM("menu.settings.window.customSize"),
    FULLSCREEN("menu.settings.window.fullscreenSize");

    private final String textKey;

    WindowMode(String textKey) {
        this.textKey = textKey;
    }

    public String getTextKey() {
        return textKey;
    }
}