<img src="https://github.com/Ramotion/folding-cell/blob/master/header.png">

<a href="https://github.com/Ramotion/fluid-slider-android">
<img align="left" src="https://github.com/Ramotion/fluid-slider-android/blob/master/Fluid_slider.gif" width="480" height="360" /></a>

<p><h1 align="left">FLUID SLIDER [KOTLIN]</h1></p>

<h4>A slider widget with a popup bubble displaying the precise value selected</h4>


___


<p><h6>We specialize in the designing and coding of custom UI for Mobile Apps and Websites.</h6>
<a href="https://dev.ramotion.com?utm_source=gthb&utm_medium=repo&utm_campaign=fluid-slider-android">
<img src="https://github.com/ramotion/gliding-collection/raw/master/contact_our_team@2x.png" width="187" height="34"></a>
</p>
<p><h6>Stay tuned for the latest updates:</h6>
<a href="https://goo.gl/rPFpid" >
<img src="https://i.imgur.com/ziSqeSo.png/" width="156" height="28"></a></p>
<h6><a href="https://store.ramotion.com/product/samsung-clay-mockups?utm_source=gthb&utm_medium=special&utm_campaign=fluid-slider-android#demo">Get Free Samsung Mockup For your project →</a></h6>

Inspired by [Virgil Pana](https://dribbble.com/virgilpana) [shot](https://dribbble.com/shots/3868232-Fluid-Slider)

</br>

[![Twitter](https://img.shields.io/badge/Twitter-@Ramotion-blue.svg?style=flat)](http://twitter.com/Ramotion)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/92bd2e49f7e543cd8748c670b9e52ca7)](https://www.codacy.com/app/dvg4000/fluid-slider-android?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Ramotion/fluid-slider-android&amp;utm_campaign=Badge_Grade)
[![Donate](https://img.shields.io/badge/Donate-PayPal-blue.svg)](https://paypal.me/Ramotion)

## Requirements

- Android 4.1 Jelly Bean (API lvl 16) or greater
- Your favorite IDE

## Installation
​
Just download the package from [here](http://central.maven.org/maven2/com/ramotion/fluidslider/fluid-slider/0.3.0/fluid-slider-0.3.0.aar) and add it to your project classpath, or just use the maven repo:

Gradle:
```groovy
implementation 'com.ramotion.fluidslider:fluid-slider:0.3.0'
```
SBT:
```scala
libraryDependencies += "com.ramotion.fluidslider" % "fluid-slider" % "0.3.0"
```
Maven:
```xml
<dependency>
  <groupId>com.ramotion.fluidslider</groupId>
  <artifactId>fluid-slider</artifactId>
  <version>0.3.0</version>
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

Here is simple example, how to change `FluidSlider` range.
```kotlin
// Kotlin
val max = 45
val min = 10
val total = max - min

val slider = findViewById<FluidSlider>(R.id.fluidSlider)
slider.positionListener = { pos -> slider.bubbleText = "${min + (total  * pos).toInt()}" }
slider.position = 0.3f
slider.startText ="$min"
slider.endText = "$max"

// Java
final FluidSlider slider = findViewById(R.id.fluidSlider);
slider.setBeginTrackingListener(new Function0<Unit>() {
    @Override
    public Unit invoke() {
        Log.d("D", "setBeginTrackingListener");
        return Unit.INSTANCE;
    }
});

slider.setEndTrackingListener(new Function0<Unit>() {
    @Override
    public Unit invoke() {
        Log.d("D", "setEndTrackingListener");
        return Unit.INSTANCE;
    }
});

// Or Java 8 lambda
slider.setPositionListener(pos -> {
    final String value = String.valueOf( (int)((1 - pos) * 100) );
    slider.setBubbleText(value);
    return Unit.INSTANCE;
});
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


This library is a part of a <a href="https://github.com/Ramotion/android-ui-animation-components-and-libraries"><b>selection of our best UI open-source projects.</b></a>

## Third Party Bindings
 ### React Native
You may now use this library with [React Native](https://github.com/facebook/react-native) via the module [here](https://github.com/prscX/react-native-fluidic-slider)

## 🗂 Check this library on other language:
<a href="https://github.com/Ramotion/fluid-slider">
<img src="https://github.com/ramotion/navigation-stack/raw/master/Swift@2x.png" width="178" height="81"></a>


## 📄 License

Fluid Slider Android is released under the MIT license.
See [LICENSE](./LICENSE) for details.

This library is a part of a <a href="https://github.com/Ramotion/android-ui-animation-components-and-libraries"><b>selection of our best UI open-source projects</b></a>

If you use the open-source library in your project, please make sure to credit and backlink to www.ramotion.com

## 📱 Get the Showroom App for Android to give it a try
Try this UI component and more like this in our Android app. Contact us if interested.

<a href="https://play.google.com/store/apps/details?id=com.ramotion.showroom" >
<img src="https://raw.githubusercontent.com/Ramotion/react-native-circle-menu/master/google_play@2x.png" width="104" height="34"></a>

<a href="https://dev.ramotion.com?utm_source=gthb&utm_medium=repo&utm_campaign=fluid-slider-android">
<img src="https://github.com/ramotion/gliding-collection/raw/master/contact_our_team@2x.png" width="187" height="34"></a>

