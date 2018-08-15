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

package androidx.core.os;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

@SmallTest
public final class HandlerCompatTest {
    private final HandlerThread mThread = new HandlerThread("handler-compat-test");

    @Before public void before() {
        mThread.start();
    }

    @After public void after() {
        assertTrue(mThread.quit());
    }

    @Test public void postDelayedWithToken() throws InterruptedException {
        final Handler handler = new Handler(mThread.getLooper());

        // Schedule a latch at 300ms to block the test thread.
        final CountDownLatch latch = new CountDownLatch(1);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                latch.countDown();
            }
        }, 300);

        final List<String> events = new ArrayList<>();
        final Object token = new Object();

        // Schedule an event at 200ms with the token.
        HandlerCompat.postDelayed(handler, new Runnable() {
            @Override
            public void run() {
                events.add("200");
            }
        }, token, 200);

        // Schedule an event at 100ms which removes future messages with the token.
        HandlerCompat.postDelayed(handler, new Runnable() {
            @Override
            public void run() {
                events.add("100");
                handler.removeCallbacksAndMessages(token);
            }
        }, token, 100);

        // Schedule an event immediately to ensure the delays are being honored.
        handler.post(new Runnable() {
            @Override
            public void run() {
                events.add("0");
            }
        });

        assertTrue(latch.await(1, SECONDS));
        assertEquals(asList("0", "100"), events);
    }

    @Test public void createAsyncAllApiLevels() throws InterruptedException {
        Handler handler = HandlerCompat.createAsync(mThread.getLooper());

        final CountDownLatch latch = new CountDownLatch(1);
        Message message = Message.obtain(handler, new Runnable() {
            @Override
            public void run() {
                latch.countDown();
            }
        });

        handler.sendMessage(message);

        assertTrue(latch.await(1, SECONDS));
    }

    @SdkSuppress(minSdkVersion = 16)
    @Test public void createAsyncWhenAsyncAvailable() throws InterruptedException {
        Handler handler = HandlerCompat.createAsync(mThread.getLooper());

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Boolean> isAsync = new AtomicReference<>();
        final AtomicReference<Message> self = new AtomicReference<>();
        Message message = Message.obtain(handler, new Runnable() {
            @Override
            public void run() {
                isAsync.set(MessageCompat.isAsynchronous(self.get()));
                latch.countDown();
            }
        });
        self.set(message);

        handler.sendMessage(message);

        assertTrue(latch.await(1, SECONDS));
        assertTrue(isAsync.get());
    }

    @Test public void createAsyncWithCallbackAllApiLevels() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Handler handler = HandlerCompat.createAsync(mThread.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                latch.countDown();
                return true;
            }
        });

        handler.sendEmptyMessage(0);
        assertTrue(latch.await(1, SECONDS));
    }

    @SdkSuppress(minSdkVersion = 16)
    @Test public void createAsyncWithCallbackWhenAsyncAvailable() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Boolean> isAsync = new AtomicReference<>();
        Handler handler = HandlerCompat.createAsync(mThread.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                isAsync.set(MessageCompat.isAsynchronous(msg));
                latch.countDown();
                return true;
            }
        });

        handler.sendEmptyMessage(0);
        assertTrue(latch.await(1, SECONDS));
        assertTrue(isAsync.get());
    }
}