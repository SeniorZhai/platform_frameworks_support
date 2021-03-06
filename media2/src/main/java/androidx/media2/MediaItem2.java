/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.media2;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.os.Bundle;
import android.os.ParcelUuid;
import android.text.TextUtils;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.NonParcelField;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.UUID;

/**
 * A class with information on a single media item with the metadata information.
 * Media item are application dependent so we cannot guarantee that they contain the right values.
 * <p>
 * When it's sent to a controller or browser, it's anonymized and data descriptor wouldn't be sent.
 * <p>
 * This object isn't a thread safe.
 */
@VersionedParcelize
public class MediaItem2 implements VersionedParcelable {
    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true, value = { FLAG_BROWSABLE, FLAG_PLAYABLE })
    public @interface Flags { }

    /**
     * Flag: Indicates that the item has children of its own.
     */
    public static final int FLAG_BROWSABLE = 1 << 0;

    /**
     * Flag: Indicates that the item is playable.
     * <p>
     * The id of this item may be passed to
     * {@link MediaController2#playFromMediaId(String, Bundle)}
     */
    public static final int FLAG_PLAYABLE = 1 << 1;

    private static final String KEY_ID = "android.media.mediaitem2.id";
    private static final String KEY_FLAGS = "android.media.mediaitem2.flags";
    private static final String KEY_METADATA = "android.media.mediaitem2.metadata";
    private static final String KEY_UUID = "android.media.mediaitem2.uuid";

    @ParcelField(1)
    String mId;
    @ParcelField(2)
    int mFlags;
    @ParcelField(3)
    ParcelUuid mParcelUuid;
    @ParcelField(4)
    MediaMetadata2 mMetadata;

    @NonParcelField
    private DataSourceDesc2 mDataSourceDesc;

    /**
     * Used for VersionedParcelable
     */
    MediaItem2() {
    }

    MediaItem2(@Nullable String mediaId, @Nullable DataSourceDesc2 dsd,
            @Nullable MediaMetadata2 metadata, @Flags int flags) {
        this(mediaId, dsd, metadata, flags, null);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaItem2(@Nullable String mediaId, @Nullable DataSourceDesc2 dsd,
            @Nullable MediaMetadata2 metadata, @Flags int flags, @Nullable ParcelUuid parcelUuid) {
        if (metadata != null && !TextUtils.equals(mediaId, metadata.getMediaId())) {
            throw new IllegalArgumentException("metadata's id should be matched with the mediaid");
        }

        mId = mediaId;
        mDataSourceDesc = dsd;
        mMetadata = metadata;
        mFlags = flags;
        mParcelUuid = parcelUuid == null ? new ParcelUuid(UUID.randomUUID()) : parcelUuid;
    }

    /**
     * Return this object as a bundle to share between processes.
     *
     * @return a new bundle instance
     * @hide
     */
    public @NonNull Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_ID, mId);
        bundle.putInt(KEY_FLAGS, mFlags);
        if (mMetadata != null) {
            bundle.putBundle(KEY_METADATA, mMetadata.toBundle());
        }
        bundle.putParcelable(KEY_UUID, mParcelUuid);
        return bundle;
    }

    /**
     * Create a MediaItem2 from the {@link Bundle}.
     *
     * @param bundle The bundle which was published by {@link MediaItem2#toBundle()}.
     * @return The newly created MediaItem2. Can be {@code null} for {@code null} bundle.
     * @hide
     */
    public static @Nullable MediaItem2 fromBundle(@Nullable Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        final ParcelUuid parcelUuid = bundle.getParcelable(KEY_UUID);
        return fromBundle(bundle, parcelUuid);
    }

    /**
     * Create a MediaItem2 from the {@link Bundle} with the specified {@link UUID} string.
     * <p>
     * {@link UUID} string can be null if it want to generate new one.
     *
     * @param bundle The bundle which was published by {@link MediaItem2#toBundle()}.
     * @param parcelUuid A {@link ParcelUuid} string to override. Can be {@link null} for override.
     * @return The newly created MediaItem2
     */
    static MediaItem2 fromBundle(@NonNull Bundle bundle, @Nullable ParcelUuid parcelUuid) {
        if (bundle == null) {
            return null;
        }
        final String id = bundle.getString(KEY_ID);
        final Bundle metadataBundle = bundle.getBundle(KEY_METADATA);
        final MediaMetadata2 metadata = metadataBundle != null
                ? MediaMetadata2.fromBundle(metadataBundle) : null;
        final int flags = bundle.getInt(KEY_FLAGS);
        return new MediaItem2(id, null, metadata, flags, parcelUuid);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MediaItem2{");
        sb.append("mId=").append(mId);
        sb.append(", mFlags=").append(mFlags);
        sb.append(", mMetadata=").append(mMetadata);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Gets the flags of the item.
     */
    public @Flags int getFlags() {
        return mFlags;
    }

    /**
     * Returns whether this item is browsable.
     * @see #FLAG_BROWSABLE
     */
    public boolean isBrowsable() {
        return (mFlags & FLAG_BROWSABLE) != 0;
    }

    /**
     * Returns whether this item is playable.
     * @see #FLAG_PLAYABLE
     */
    public boolean isPlayable() {
        return (mFlags & FLAG_PLAYABLE) != 0;
    }

    /**
     * Set a metadata. If the metadata is not null, its id should be matched with this instance's
     * media id.
     *
     * @param metadata metadata to update
     */
    public void setMetadata(@Nullable MediaMetadata2 metadata) {
        if (metadata != null && !TextUtils.equals(mId, metadata.getMediaId())) {
            throw new IllegalArgumentException("metadata's id should be matched with the mediaId");
        }
        mMetadata = metadata;
    }

    /**
     * Returns the metadata of the media.
     *
     * @return metadata from the session
     */
    public @Nullable MediaMetadata2 getMetadata() {
        return mMetadata;
    }

    /**
     * Returns the media id for this item. If it's not {@code null}, it's a persistent unique key
     * for the underlying media content.
     *
     * @return media Id from the session
     */
    public @Nullable String getMediaId() {
        return mId;
    }

    /**
     * Return the {@link DataSourceDesc2}
     * <p>
     * Can be {@code null} if the MediaItem2 came from another process and anonymized
     *
     * @return data source descriptor
     */
    public @Nullable DataSourceDesc2 getDataSourceDesc() {
        return mDataSourceDesc;
    }

    @Override
    public int hashCode() {
        return mParcelUuid.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof MediaItem2)) {
            return false;
        }
        MediaItem2 other = (MediaItem2) obj;
        return mParcelUuid.equals(other.mParcelUuid);
    }

    UUID getUuid() {
        return mParcelUuid.getUuid();
    }

    /**
     * Builder for {@link MediaItem2}
     */
    public static final class Builder {
        private @Flags int mFlags;
        private String mMediaId;
        private MediaMetadata2 mMetadata;
        private DataSourceDesc2 mDataSourceDesc;
        private UUID mUuid;

        /**
         * Constructor for {@link Builder}
         *
         * @param flags
         */
        public Builder(@Flags int flags) {
            mFlags = flags;
        }

        /**
         * Set the media id of this instance. {@code null} for unset.
         * <p>
         * If used, this should be a persistent unique key for the underlying content so session
         * and controller can uniquely identify a media content.
         * <p>
         * If the metadata is set with the {@link #setMetadata(MediaMetadata2)} and it has
         * media id, id from {@link #setMediaId(String)} will be ignored and metadata's id will be
         * used instead.
         *
         * @param mediaId media id
         * @return this instance for chaining
         */
        public @NonNull Builder setMediaId(@Nullable String mediaId) {
            mMediaId = mediaId;
            return this;
        }

        /**
         * Set the metadata of this instance. {@code null} for unset.
         * <p>
         * If the metadata is set with the {@link #setMetadata(MediaMetadata2)} and it has
         * media id, id from {@link #setMediaId(String)} will be ignored and metadata's id will be
         * used instead.
         *
         * @param metadata metadata
         * @return this instance for chaining
         */
        public @NonNull Builder setMetadata(@Nullable MediaMetadata2 metadata) {
            mMetadata = metadata;
            return this;
        }

        /**
         * Set the data source descriptor for this instance. {@code null} for unset.
         *
         * @param dataSourceDesc data source descriptor
         * @return this instance for chaining
         */
        public @NonNull Builder setDataSourceDesc(@Nullable DataSourceDesc2 dataSourceDesc) {
            mDataSourceDesc = dataSourceDesc;
            return this;
        }

        Builder setUuid(UUID uuid) {
            mUuid = uuid;
            return this;
        }

        /**
         * Build {@link MediaItem2}.
         *
         * @return a new {@link MediaItem2}.
         */
        public @NonNull MediaItem2 build() {
            String id = (mMetadata != null)
                    ? mMetadata.getString(MediaMetadata2.METADATA_KEY_MEDIA_ID) : null;
            if (id == null) {
                id = mMediaId;
            }
            return new MediaItem2(id, mDataSourceDesc, mMetadata, mFlags,
                    mUuid == null ? null : new ParcelUuid(mUuid));
        }
    }
}
