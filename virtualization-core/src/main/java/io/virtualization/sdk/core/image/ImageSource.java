package io.virtualization.sdk.core.image;

/** Where {@link ImageProvider#importImage(ImageSource)} reads image data from. */
public sealed interface ImageSource
        permits LocalFileImageSource, InputStreamImageSource, URLImageSource, RemoteImageSource {}
