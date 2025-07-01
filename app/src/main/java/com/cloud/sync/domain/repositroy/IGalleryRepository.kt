package com.cloud.sync.domain.repositroy

import com.cloud.sync.domain.model.GalleryPhoto

/**
 * Interface for accessing photos from the device's gallery.
 */
interface IGalleryRepository {
    /**
     * Get photos from the gallery starting from a specific timestamp (seconds).
     * @param startTimeSeconds: Time from which to fetch photos (default is 0 for all).
     * @return List<GalleryPhoto>: A list of photos matching the time filter.
     */
    fun getPhotos(startTimeSeconds: Long = 0): List<GalleryPhoto>

    /**
     * Get photos from the gallery within a specific time interval.
     * @param start: Start timestamp in seconds.
     * @param end: End timestamp in seconds.
     * @return List<GalleryPhoto>: A list of photos within the given interval.
     */
    fun getPhotosInInterval(start: Long, end: Long): List<GalleryPhoto>

}
