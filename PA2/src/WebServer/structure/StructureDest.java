package WebServer.structure;

public class StructureDest {
    private String name;
    private String type;
    private String description;
    private String image;

    public StructureDest(
        String name,
        String type,
        String description,
        String image
    ) {
        this.name = name;
        this.description = description;
        this.image = image;
        this.type = type;
    }

    public StructureDest(StructureDest other) {
        this.name = other.name;
        this.type = other.type;
        this.description = other.description;
        this.image = other.image;
    }

    public String getName() {
        return name;
    }
    public String getType() {
        return type;
    }
    public String getDescription() {
        return description;
    }
    public String getImage() {
        return image;
    }
}
