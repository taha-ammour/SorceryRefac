# 2D Game Engine

A flexible and feature-rich 2D game engine built with Java and LWJGL, featuring an Entity Component System (ECS) architecture, audio, networking, and more.

## Features

- **Entity Component System**: Modular architecture for game object management
- **OpenGL Rendering**: Sprite-based 2D graphics with shader support
- **Audio System**: 3D positional audio with OpenAL
- **Input Handling**: Keyboard, mouse, and gamepad support
- **Collision Detection**: AABB collision with resolution
- **Networking**: Multiplayer support with client-server model
- **UI Framework**: Text rendering and UI components
- **Animation**: Frame-based sprite animations
- **Lighting**: 2D lighting with multiple light types
- **Particle System**: For visual effects
- **Resource Management**: For textures, audio, and other assets

## Requirements

- Java 11 or higher
- Gradle 7.0 or higher
- LWJGL 3.3.6 (included as dependency)
- OpenGL 3.3 capable graphics card

## Setup

1. Clone the repository
2. Navigate to the project directory
3. Run `gradle build` to build the project
4. Run `gradle run` to launch the demo

## Project Structure

```
src/main/java/org/example/
├── engine/             # Core engine components
│   ├── ecs/            # Entity Component System
│   │   ├── components/ # Component definitions
│   │   └── systems/    # System implementations
│   └── ui/             # UI framework
├── game/               # Game-specific code
└── Main.java           # Entry point
```

## Quick Start

### Initialize the Engine

```java
Engine engine = new Engine();
engine.init(800, 600, "Game Title");

Scene scene = new Scene();
engine.setActiveScene(scene);

// Start the game loop
engine.run();
```

### Create a Game Object

```java
Sprite sprite = spriteManager.getSprite("player");
sprite.setPosition(100, 100);
scene.addGameObject(sprite);
```

### Create an Entity with Components

```java
Entity entity = new Entity();
entity.addComponent(new TransformComponent(x, y, z));
entity.addComponent(new SpriteComponent(sprite));
entity.addComponent(new MovementComponent(speed));
ECSManager.getInstance().addEntity(entity);
```

### Play Sounds

```java
// Create audio engine and system
OpenALAudioEngine audioEngine = new OpenALAudioEngine();
AudioSystem audioSystem = new AudioSystem(audioEngine);
ECSManager.getInstance().addSystem(audioSystem);

// Load sound
audioEngine.loadSound("explosion", "/sounds/explosion.wav");

// Play sound
audioEngine.playSound(
    "explosion",
    x, y, z,      // position
    0.8f,         // volume
    1.0f,         // pitch
    false,        // not looping
    true,         // 3D positioned
    10.0f, 100.0f // distance settings
);
```

### Handle Input

```java
if (input.isKeyDown(GLFW_KEY_W)) {
    // Move up
}
if (input.isKeyJustPressed(GLFW_KEY_SPACE)) {
    // Jump once
}
```

## Systems Overview

### Audio System

The audio system provides 2D and 3D positional audio using OpenAL. It supports:

- WAV file playback
- Volume and pitch control
- Looping sounds
- 3D positioning with distance attenuation
- Entity-based sound sources

```java
// Add an audio component to an entity
AudioComponent audioComp = new AudioComponent("sound_id");
audioComp.setVolume(0.7f);
audioComp.setLooping(true);
entity.addComponent(audioComp);

// Play the sound
audioSystem.playSound(entity);
```

### Rendering System

The rendering system draws sprites with support for:

- Sprite sheets and texture atlases
- Palette swapping for color variation
- Z-ordering for proper layering
- Custom shaders for effects
- Batched rendering for performance

```java
// Create and configure a sprite
Sprite sprite = spriteManager.getSprite("sprite_name");
sprite.setPosition(x, y);
sprite.setScale(2.0f, 2.0f);
sprite.setZ(10);
sprite.setPaletteFromCodes(new String[]{"000", "500", "300", "555"});
```

### Entity Component System

The ECS architecture separates data (Components) from behavior (Systems):

```java
// Create a System that processes entities with specific components
public class MySystem extends System {
    public MySystem() {
        super(10, TransformComponent.class, MovementComponent.class);
    }
    
    @Override
    public void processEntity(Entity entity, float deltaTime) {
        TransformComponent transform = entity.getComponent(TransformComponent.class);
        MovementComponent movement = entity.getComponent(MovementComponent.class);
        
        // Process entity
    }
}

// Add the system to the ECS manager
ECSManager.getInstance().addSystem(new MySystem());
```

### UI System

The UI system provides components for user interfaces:

```java
// Create UI text
FontSheet fontSheet = new FontSheet();
Shader fontShader = Shader.loadFromFiles("/shaders/sprite.vs.glsl", "/shaders/Fontsh.fs.glsl");
UIText text = new UIText(fontSheet, fontShader, "Hello World", 100, 100);
uiManager.addComponent(text, true);

// Create UI panel with layout
UIPanel panel = new UIPanel(x, y, width, height);
panel.setLayout(UIPanel.Layout.VERTICAL, 10.0f);
panel.addComponent(new UISprite(0, 0, 32, 32, "button", spriteManager));
uiManager.addComponent(panel, false);
```

### Networking System

The networking system enables multiplayer gameplay:

```java
// Create a server
Server server = GameNetworking.createServer(serverName, hostUsername, tcpPort, udpPort);

// Create a client and connect
Client client = GameNetworking.createClient(serverIp, tcpPort, udpPort);

// Set up listeners
GameNetworking.setupClientListeners(client, gameWorld);

// Send a message
client.sendTCP(new Packets.ChatMessage(username, message));
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request