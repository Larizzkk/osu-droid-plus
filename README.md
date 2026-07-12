![osudroid](https://github.com/osudroid/osu-droid/assets/52914632/8747ab50-c77d-4808-91aa-5058891e4bcd)

# osu-droid

osudroid!plus is a [fork of a free-to-play circle clicking rhythm game for Android devices](https://github.com/osudroid/osu-droid). It was a game hatched many years ago by the
[osu!](https://osu.ppy.sh/home) community. This project is now being developed by a small group of fans and also with the help of foreign friends.

## Status

osu!droid is under constant development. Features are constantly being added and bugs are constantly being fixed, but
it's playable and fun!

### Downloading the source code

Clone the repository:

```sh
git clone https://github.com/Larizzkk/osu-droid-plus
```
Open the folder in Android Studio.

To update the source code to the latest commit, run the following command inside the `osu-droid-plus` directory:

```she
git pull
```

### Building 
<!--
TODO: make a building tutorial
i believe it's the same process?
--->

osu!droid requires Java 17 to build.

In Android Studio, you can `Build` a debug release to test your changes. The output directory of your `.apk` is inside
`build/output` of `osu-droid-plus`'s directory.

If you prefer the command line, and you are on Linux, run `chmod +x gradlew` and `./gradlew assembleDebug` inside the
directory to build the debug `.apk` files.

## Contributing

We welcome any sort of contributions, as long as they're helpful. Those who aren't able to contribute code may instead
suggest small changes like grammar fixes or report client issues via [Feature request](https://github.com/Larizzkk/osu-droid-plus/issues) (New issue --> Feature Request) or [GitHub Issues](https://github.com/Larizzkk/osu-droid-plus/issues)

## License

osu!droid is licensed under the [Apache-2.0](https://opensource.org/licenses/Apache-2.0) License. Please see the LICENSE file for more information.
