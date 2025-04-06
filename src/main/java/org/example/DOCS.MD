# Game Engine Documentation

## Overview

This is a 2D game engine built with Java and LWJGL. It features a modular architecture with an Entity Component System (ECS), rendering system, input handling, audio system, and networking capabilities.

### Core Features

- **Entity Component System (ECS)**: Flexible and modular game object management
- **Rendering**: Sprite-based 2D rendering with palette switching support
- **Audio**: 3D positional audio using OpenAL
- **Input**: Keyboard, mouse, and gamepad support
- **Physics**: Basic collision detection and resolution
- **Networking**: Multiplayer support with client-server architecture
- **UI**: Text rendering, panels, and basic UI components
- **Animation**: Frame-based sprite animations
- **Lighting**: 2D lighting system with different light types
- **Particles**: Particle system for effects

## Architecture

The engine is built around several key systems:

### Core Components

- **Engine**: Main entry point that manages the game loop, window, and core systems
- **Scene**: Container for game objects with layered rendering
- **GameObject**: Base class for all objects in the scene
- **Camera**: 2D orthographic camera with zoom and rotation
- **Input**: Input handling for keyboard, mouse, and gamepad
- **Shader**: OpenGL shader management

### Entity Component System (ECS)

The ECS architecture separates data (Components) from behavior (Systems):

- **Entity**: Container for components with a unique ID
- **Component**: Data container for specific aspects (transform, sprite, audio, etc.)
- **System**: Processes entities with specific component requirements

### Key Systems

- **RenderSystem**: Handles drawing entities with sprite components
- **AudioSystem**: Manages sound playback and 3D positioning
- **CollisionSystem**: Detects and resolves collisions between entities
- **AnimationSystem**: Updates sprite animations
- **NetworkSystem**: Syncs entity states between clients

## Systems Detail

### Rendering System

The rendering system uses OpenGL to draw sprites from texture atlases. It supports:

- Sprite sheets with sub-region extraction
- Palette swapping for retro-style color changes
- Z-ordering for proper layering
- Batched rendering for efficiency
- Custom shaders for special effects

### Audio System

The audio system uses OpenAL to provide both 2D and 3D positional audio:

- Supports WAV file format
- 3D audio with distance attenuation
- Volume and pitch control
- Looping sounds
- Auto-playing sounds linked to entities

#### AudioEngine Interface

The `AudioEngine` interface defines the following methods:

```java
int playSound(String soundId, float x, float y, float z, float volume, float pitch,
              boolean looping, boolean is3D, float minDistance, float maxDistance);
boolean isPlaying(int sourceId);
void stopSound(int sourceId);
void setSourcePosition(int sourceId, float x, float y, float z);
void setSourceVolume(int sourceId, float volume);
void setSourcePitch(int sourceId, float pitch);
void setListenerPosition(float x, float y, float z);
void loadSound(String soundId, String filePath);
void unloadSound(String soundId);
```

#### OpenALAudioEngine Implementation

The `OpenALAudioEngine` class implements the `AudioEngine` interface using OpenAL:

- Manages audio device and context
- Loads WAV files into buffers
- Creates and configures audio sources
- Handles 3D audio positioning
- Cleans up resources properly

#### AudioSystem Integration with ECS

The `AudioSystem` class extends the ECS `System` and processes entities with `AudioComponent`:

- Updates sound positions based on entity transforms
- Automatically plays sounds with autoPlay flag
- Handles sound looping

#### AudioComponent

The `AudioComponent` class holds audio configuration for an entity:

```java
private String soundId;
private float volume = 1.0f;
private float pitch = 1.0f;
private boolean isLooping = false;
private boolean autoPlay = false;
private float minDistance = 10.0f;
private float maxDistance = 100.0f;
private boolean is3D = true;
```

### Input System

The input system provides a clean interface for handling keyboard, mouse, and gamepad input:

- Key pressed/released/held state tracking
- Mouse position and button state
- Scroll wheel support
- Gamepad axes and buttons

### Network System

The networking system uses KryoNet for client-server communication:

- Player joining/leaving
- Position updates
- Chat messages
- Host migration
- Server discovery

### UI System

The UI system provides basic user interface components:

- Text rendering with custom fonts
- Panels with different layout options
- Sprites and buttons
- Layered rendering

### Lighting System

The lighting system adds dynamic lighting to the 2D world:

- Directional lights
- Point lights
- Spotlights
- Normal mapping support

### Collision System

The collision system handles basic physics:

- AABB collision detection
- Collision resolution
- Layer-based filtering

## Using the Engine

### Setting Up

The engine is initialized with the following code:

```java
Engine engine = new Engine();
engine.init(800, 600, "Game Window");

Scene scene = new Scene();
engine.setActiveScene(scene);

// Add game objects to the scene
scene.addGameObject(new GameObject());

engine.run();
```

### Creating an Entity with Components

```java
// Create a new entity
Entity entity = new Entity();

// Add a transform component
TransformComponent transform = new TransformComponent();
transform.setPosition(100, 100, 0);
entity.addComponent(transform);

// Add a sprite component
SpriteComponent sprite = new SpriteComponent(spriteManager.getSprite("player"));
entity.addComponent(sprite);

// Add an audio component
AudioComponent audio = new AudioComponent("footstep");
audio.setVolume(0.5f);
audio.setLooping(false);
entity.addComponent(audio);

// Add the entity to the ECS manager
ECSManager.getInstance().addEntity(entity);
```

### Playing Sound with the Audio System

#### Creating an Entity with an Audio Component

```java
Entity audioEntity = new Entity("sound_entity");
audioEntity.addComponent(new TransformComponent(x, y, z));

AudioComponent audioComp = new AudioComponent("sound_id");
audioComp.setVolume(0.8f);
audioComp.setPitch(1.0f);
audioComp.setLooping(false);
audioComp.setAutoPlay(true); // Will play automatically when processed
audioEntity.addComponent(audioComp);

ECSManager.getInstance().addEntity(audioEntity);
```

#### Playing Sounds

```java
// Play a sound on an entity through AudioSystem
Entity entity = ECSManager.getInstance().getEntity(entityId);
audioSystem.playSound(entity);

// Play a sound directly through AudioEngine
int sourceId = audioEngine.playSound(
    "sound_id",
    x, y, z,         // position
    0.8f,            // volume
    1.0f,            // pitch
    false,           // not looping
    true,            // 3D
    10.0f, 100.0f    // min/max distance
);

// Stop a sound
audioEngine.stopSound(sourceId);
```

#### Updating Listener Position

```java
// Update listener position to follow player
Vector3f playerPos = player.getPosition();
audioEngine.setListenerPosition(playerPos.x, playerPos.y, playerPos.z);
```

### Creating a Player

```java
// Create a player with the PlayerFactory
Player player = gameWorld.createLocalPlayer("Username", "RED");

// Move the player
if (input.isKeyDown(GLFW_KEY_W)) {
    player.move(Direction.UP, deltaTime);
}
```

### Setting Up Networking

```java
// Create a server
Server server = GameNetworking.createServer("Server Name", "Host Username", tcpPort, udpPort);

// Create a client and connect to server
Client client = GameNetworking.createClient("server_ip", tcpPort, udpPort);

// Set up the game world with networking
gameWorld.setupNetworking(client);
```

## Example: Complete Audio Implementation

The following example demonstrates how to set up and use the audio system in the game engine:

```java
// Initialize audio system
OpenALAudioEngine audioEngine = new OpenALAudioEngine();
AudioSystem audioSystem = new AudioSystem(audioEngine);
ECSManager.getInstance().addSystem(audioSystem);

// Load sounds
audioEngine.loadSound("footstep", "/sounds/footstep.wav");
audioEngine.loadSound("ambient", "/sounds/ambient.wav");
audioEngine.loadSound("explosion", "/sounds/explosion.wav");

// Create an entity with ambient sound
Entity ambientSoundEntity = new Entity();
ambientSoundEntity.addComponent(new TransformComponent(x, y, z));

AudioComponent ambientAudio = new AudioComponent("ambient");
ambientAudio.setVolume(0.2f);
ambientAudio.setPitch(1.0f);
ambientAudio.setLooping(true);
ambientAudio.setAutoPlay(true);
ambientAudio.set3D(false);
ambientSoundEntity.addComponent(ambientAudio);

ECSManager.getInstance().addEntity(ambientSoundEntity);

// Play a positional sound at player location
Vector3f playerPos = player.getPosition();
audioEngine.playSound(
    "footstep",
    playerPos.x, playerPos.y, playerPos.z,
    0.5f,        // volume
    1.0f,        // pitch
    false,       // not looping
    true,        // 3D
    10.0f, 50.0f // min/max distance
);

// Update listener position each frame
audioEngine.setListenerPosition(camera.getPosition().x, camera.getPosition().y, camera.getPosition().z);

// Clean up audio resources when done
audioEngine.cleanup();
```

This engine provides a solid foundation for 2D game development with all the essential systems needed to create fully featured games.