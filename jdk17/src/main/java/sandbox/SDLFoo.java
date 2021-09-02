/*
 * MIT License
 *
 * Copyright (c) 2021 Brice Dutheil <brice.dutheil@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package sandbox;

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import sdl2.SDL_Event;
import sdl2.SDL_TextInputEvent;

import java.util.Objects;

import static jdk.incubator.foreign.MemoryAddress.NULL;
import static sdl2.LibSDL2.GL_COLOR_BUFFER_BIT;
import static sdl2.LibSDL2.GL_MODELVIEW;
import static sdl2.LibSDL2.GL_NO_ERROR;
import static sdl2.LibSDL2.GL_PROJECTION;
import static sdl2.LibSDL2.GL_QUADS;
import static sdl2.LibSDL2.SDL_CreateWindow;
import static sdl2.LibSDL2.SDL_DestroyWindow;
import static sdl2.LibSDL2.SDL_GL_CONTEXT_MAJOR_VERSION;
import static sdl2.LibSDL2.SDL_GL_CONTEXT_MINOR_VERSION;
import static sdl2.LibSDL2.SDL_GL_CreateContext;
import static sdl2.LibSDL2.SDL_GL_SetAttribute;
import static sdl2.LibSDL2.SDL_GL_SetSwapInterval;
import static sdl2.LibSDL2.SDL_GL_SwapWindow;
import static sdl2.LibSDL2.SDL_GetError;
import static sdl2.LibSDL2.SDL_INIT_VIDEO;
import static sdl2.LibSDL2.SDL_Init;
import static sdl2.LibSDL2.SDL_PollEvent;
import static sdl2.LibSDL2.SDL_QUIT;
import static sdl2.LibSDL2.SDL_Quit;
import static sdl2.LibSDL2.SDL_StartTextInput;
import static sdl2.LibSDL2.SDL_StopTextInput;
import static sdl2.LibSDL2.SDL_TEXTINPUT;
import static sdl2.LibSDL2.SDL_WINDOWPOS_UNDEFINED;
import static sdl2.LibSDL2.SDL_WINDOW_OPENGL;
import static sdl2.LibSDL2.SDL_WINDOW_SHOWN;
import static sdl2.LibSDL2.glBegin;
import static sdl2.LibSDL2.glClear;
import static sdl2.LibSDL2.glClearColor;
import static sdl2.LibSDL2.glColor3f;
import static sdl2.LibSDL2.glEnd;
import static sdl2.LibSDL2.glGetError;
import static sdl2.LibSDL2.glLoadIdentity;
import static sdl2.LibSDL2.glMatrixMode;
import static sdl2.LibSDL2.glRotatef;
import static sdl2.LibSDL2.glVertex2f;

/**
 * Example from <a href="https://lazyfoo.net/tutorials/SDL/50_SDL_and_opengl_2/index.php">SDL and OpenGL tutorial</a>.
 *
 * To run this example :
 * - One need to install XCode on macOs
 * - {@code brew install sdl2}
 *
 * Then create a file {@code sdl-foo.h} with the following content
 * (trick to extract mapping for multiple headers):
 *
 * <pre><code>
 * #include <SDL.h>
 * #include <SDL_opengl.h>
 * </code></pre>
 *
 * Then extract the mapping from this file
 * <pre><code>
 * jextract --source -d src/main/java -t sdl2 \
 *     -I /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include \
 *     -I /usr/local/include/SDL2 -l SDL2 \
 *     --header-class-name LibSDL2 \
 *     sdl-foo.h
 * </code></pre>
 *
 * Show what the JDK defines for library lookup
 * <pre><code>
 * jshell -s - <<< "System.out.println(System.getProperty(\"java.library.path\"))"
 * </code></pre>
 *
 * Finally run the existing code that uses the above mappings.
 * <pre><code>
 * env JAVA_LIBRARY_PATH=:/usr/local/lib java \
 *   -cp build/classes/java/main \
 *   -XstartOnFirstThread \
 *   --enable-native-access=ALL-UNNAMED \
 *   --add-modules=jdk.incubator.foreign \
 *   sandbox.SDLFoo
 * </code></pre>
 */

public class SDLFoo {
  private static final int SCREEN_WIDTH = 640;
  private static final int SCREEN_HEIGHT = 480;
  private MemoryAddress gWindow;
  private MemoryAddress gContext;


  public static void main(String[] args) {
    try (var scope = ResourceScope.newConfinedScope()) {
      var sdlFoo = new SDLFoo();

      // Start up SDL and create window
      if (!sdlFoo.init(scope)) {
        System.out.println("Failed to initialize!");
        System.exit(1);
      }

      scope.addCloseAction(sdlFoo::close);

      // Event handling
      // Allocate SDL_Event sdlEvent; which is a union type
      var sdlEvent = MemorySegment.allocateNative(SDL_Event.sizeof(), scope);

      // Enable text input
      SDL_StartTextInput();

      // While application is running
      boolean quit = false;
      while (!quit) {

        // Handle events on queue
        while (SDL_PollEvent(sdlEvent) != 0) {
          // User clicked the quit button
          if (SDL_Event.type$get(sdlEvent) == SDL_QUIT()) {
            quit = true;
          }

          //#define SDL_TEXTINPUTEVENT_TEXT_SIZE (32)
          //typedef struct SDL_TextInputEvent
          //{
          //    Uint32 type;                              /**< ::SDL_TEXTINPUT */
          //    Uint32 timestamp;                         /**< In milliseconds, populated using SDL_GetTicks() */
          //    Uint32 windowID;                          /**< The window with keyboard focus, if any */
          //    char text[SDL_TEXTINPUTEVENT_TEXT_SIZE];  /**< The input text */
          //} SDL_TextInputEvent;

          //typedef union SDL_Event
          //{
          //    Uint32 type;                            /**< Event type, shared with all events */
          //    SDL_CommonEvent common;                 /**< Common event data */
          //    SDL_DisplayEvent display;               /**< Display event data */
          //    SDL_WindowEvent window;                 /**< Window event data */
          //    SDL_KeyboardEvent key;                  /**< Keyboard event data */
          //    SDL_TextEditingEvent edit;              /**< Text editing event data */
          //    SDL_TextInputEvent text;                /**< Text input event data */
          //    SDL_MouseMotionEvent motion;            /**< Mouse motion event data */
          //  ...
          //     /* This is necessary for ABI compatibility between Visual C++ and GCC.
          //       Visual C++ will respect the push pack pragma and use 52 bytes (size of
          //       SDL_TextEditingEvent, the largest structure for 32-bit and 64-bit
          //       architectures) for this union, and GCC will use the alignment of the
          //       largest datatype within the union, which is 8 bytes on 64-bit
          //       architectures.
          //
          //       So... we'll add padding to force the size to be 56 bytes for both.
          //
          //       On architectures where pointers are 16 bytes, this needs rounding up to
          //       the next multiple of 16, 64, and on architectures where pointers are
          //       even larger the size of SDL_UserEvent will dominate as being 3 pointers.
          //    */
          //    Uint8 padding[sizeof(void *) <= 8 ? 56 : sizeof(void *) == 16 ? 64 : 3 * sizeof(void *)];
          //} SDL_Event;



          // Handle keypress with current mouse position
          else if (SDL_Event.type$get(sdlEvent) == SDL_TEXTINPUT()) {
            // e.text.text[ 0 ]
            char c = CLinker.toJavaString(SDL_TextInputEvent.text$slice(sdlEvent)).charAt(0);
            if (c == 'q') {
              quit = true;
            }
          }

        }

        sdlFoo.render(scope);
        sdlFoo.update(scope);
      }

      //Disable text input
      SDL_StopTextInput();
    }
  }

  private void update(ResourceScope scope) {
    // Update a window with OpenGL rendering
    SDL_GL_SwapWindow(gWindow);
  }

  private boolean init(ResourceScope scope) {
    if (SDL_Init(SDL_INIT_VIDEO()) < 0) {
      String errMsg = CLinker.toJavaString(SDL_GetError());
      System.out.printf("SDL could not initialize! SDL Error: %s\n", errMsg);
      return false;
    } else {
      SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION(), 2);
      SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION(), 1);


      gWindow = SDL_CreateWindow(CLinker.toCString("SDL for Panama", scope),
                                 SDL_WINDOWPOS_UNDEFINED(),
                                 SDL_WINDOWPOS_UNDEFINED(),
                                 SCREEN_WIDTH,
                                 SCREEN_HEIGHT,
                                 SDL_WINDOW_OPENGL() | SDL_WINDOW_SHOWN());

      if (Objects.equals(NULL, gWindow)) {
        System.out.printf("Window could not be created! SDL Error: %s\n", CLinker.toJavaString(SDL_GetError()));
        return false;
      } else {
        // Initialize opengl
        gContext = SDL_GL_CreateContext(gWindow);
        if (Objects.equals(NULL, gContext)) {
          System.out.printf("OpenGL context could not be created! SDL Error: %s\n", CLinker.toJavaString(SDL_GetError()));
          return false;
        } else {
          //Use Vsync
          if (SDL_GL_SetSwapInterval(1) < 0) {
            System.out.printf("Warning: Unable to set VSync! SDL Error: %s\n", CLinker.toJavaString(SDL_GetError()));
          }


          //Initialize OpenGL
          if (!initGL()) {
            System.out.println("Unable to initialize OpenGL!\n");
            return false;
          }
        }
      }

      return true;
    }
  }

  private boolean initGL() {
    boolean success = true;
    int error = GL_NO_ERROR();

    // Initialize Projection Matrix
    glMatrixMode(GL_PROJECTION());
    glLoadIdentity();

    // Check for error
    error = glGetError();
    if (error != GL_NO_ERROR()) {
      success = false;
    }

    // Initialize Modelview Matrix
    glMatrixMode(GL_MODELVIEW());
    glLoadIdentity();

    // Check for error
    error = glGetError();
    if (error != GL_NO_ERROR()) {
      success = false;
    }

    // Initialize clear color
    glClearColor(0.f, 0.f, 0.f, 1.f);

    // Check for error
    error = glGetError();
    if (error != GL_NO_ERROR()) {
      success = false;
    }

    return success;
  }

  private void close() {
    SDL_DestroyWindow(gWindow);
    SDL_Quit();
  }

  private void render(ResourceScope scope) {
    //Clear color buffer
    glClear(GL_COLOR_BUFFER_BIT());

    // Rotate The cube around the Y axis
    glRotatef(0.4f,0.0f,1.0f,0.0f);
    glRotatef(0.2f,1.0f,1.0f,1.0f);
    glColor3f(0.0f,1.0f,0.0f);

    glBegin( GL_QUADS() );
      glVertex2f( -0.5f, -0.5f );
      glVertex2f( 0.5f, -0.5f );
      glVertex2f( 0.5f, 0.5f );
      glVertex2f( -0.5f, 0.5f );
    glEnd();
  }

}