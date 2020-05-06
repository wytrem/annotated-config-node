# annotate-config-node

Small library that allows annotated configurations with comments in Bukkit.

## Usage

First, create your config object:

```java
public class MercatusConfig extends AnnotatedConfigNode {
    @ConfigNode(comments = {
            "That's a comment for myInt.",
            "It's a very beautiful field.",
            "Numbers are great."
    })
    public int myInt = 9;

    @ConfigNode(comments = {"But booleans are awesome as well."})
    public boolean myFlag = true;

    @ConfigNode(comments = {"Stores the colors."})
    public MoreColors colors = new MoreColors();

    static class Colors extends AnnotatedConfigNode {
        @ConfigNode(comments = {"The green color."})
        public int green = 5;

        @ConfigNode(comments = {"The red color."})
        public int red = 9;
    }

    // Supports inherited fields.
    static class MoreColors extends Colors {
        @ConfigNode(comments = {"The yellow color."})
        public int yellow = 5;
    }
}
```

Then, load and save it:

```java
// Load config
File file = new File("/home/victor/test.yml");
CommentedYamlConfiguration commentedYaml = CommentedYamlConfiguration.loadConfiguration(AnnotatedConfigNode.getComments(MercatusConfig.class), file);
MercatusConfig config = AnnotatedConfigNode.loadAsRoot(MercatusConfig.class, commentedYaml);
System.out.println("config.colors.green = " + config.colors.green);


// Save config
config.storeAtRoot(commentedYaml);
try {
    commentedYaml.save(file);
} catch (IOException e) {
    e.printStackTrace();
}

```

Result:

```yaml
# That's a comment for myInt.
# It's a very beautiful field.
# Numbers are great.
myInt: 9

# But booleans are awesome as well.
myFlag: true

# Stores the colors.
colors:
  
  # The green color.
  green: 3
  
  # The red color.
  red: 9
  
  # The yellow color.
  yellow: 5
```
