![header](./header.png)
![animation](./Fluid-slider.gif)
## Requirements
​
- Android 4.1 Jelly Bean (API lvl 16) or greater
- Your favorite IDE

## Installation
​
Just download the package from [here]() and add it to your project classpath, or just use the maven repo:

Gradle:
```groovy
compile '???'
```
SBT:
```scala
libraryDependencies += "???"
```
Maven:
```xml
<dependency>
  <groupId>com.ramotion.fluidslider</groupId>
  <artifactId>fluid-slider</artifactId>
  <version>0.1.0</version>
  <type>aar</type>
</dependency>
```


## Basic usage

Place the `FluidSlider` in your layout.

To track the current position of the slider, set the `positionListener`, as shown below:
```
val slider = findViewById<FluidSlider>(R.id.fluidSlider)
slider.positionListener = { p -> Log.d("MainActivity", "current position is: $p" )}
```

You can also track the beginning and completion of the movement of the slider, using the following properties:
`beginTrackingListener` and` endTrackingListener`. Example below:
```
slider.beginTrackingListener = { /* action on slider touched */ }
slider.endTrackingListener = { /* action on slider released */ }
```

Here are the attributes you can specify through XML or related setters:
* `bar_color` - Color of slider.
* `bubble_color` - Color of circle "bubble" inside bar.
* `bar_text_color` - Color of `start` and `end` texts of slider.
* `bubble_text_color` - Color of text inside "bubble".
* `start_text` - Start (left) text of slider.
* `end_text` - End (right) text of slider.
* `text_size` - Text size.
* `duration` - Duration of "bubble" rise in milliseconds.
* `initial_position` - Initial positon of "bubble" in range form `0.0` to `1.0`.
* `size` - Height of slider. Can be `small` (40dp) and `normal` (56dp).

## License
​
CircleMenu for Android is released under the MIT license.
See [LICENSE](./LICENSE) for details.
