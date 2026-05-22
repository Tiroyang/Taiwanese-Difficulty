package ass.example.core;

public enum Language {
    ZH_TW("繁體中文"),
    EN_US("English");

    private final String displayName;

    Language(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}