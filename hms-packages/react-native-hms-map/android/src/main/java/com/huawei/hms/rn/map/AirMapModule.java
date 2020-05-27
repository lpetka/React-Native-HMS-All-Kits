/*Copyright (c) 2015 Airbnb

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

2020.03.05-Changed package name 
2020.03.05-Changed components which are belongs to Google replaced with Huawei
2020.03.05-Changed method getConstants
2020.03.22-Changed code style and indentation issues are fixed
Huawei Technologies Co., Ltd.
*/

package com.huawei.hms.rn.map;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.util.Base64;
import android.util.DisplayMetrics;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.uimanager.NativeViewHierarchyManager;
import com.facebook.react.uimanager.UIBlock;
import com.facebook.react.uimanager.UIManagerModule;
import com.huawei.hms.maps.HuaweiMap;
import com.huawei.hms.maps.model.CameraPosition;
import com.huawei.hms.maps.model.LatLng;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Map;
import java.util.HashMap;

import javax.annotation.Nullable;

@ReactModule(name = AirMapModule.NAME)
public class AirMapModule extends ReactContextBaseJavaModule {

    public static final String NAME = "AirMapModule";
    private static final String SNAPSHOT_RESULT_FILE = "file";
    private static final String SNAPSHOT_RESULT_BASE64 = "base64";
    private static final String SNAPSHOT_FORMAT_PNG = "png";
    private static final String SNAPSHOT_FORMAT_JPG = "jpg";
  
    public AirMapModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }
  
    @Override
    public String getName() {
        return NAME;
    }
  
    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put("legalNotice", "Open-Source");
        return constants;
    }
  
    public Activity getActivity() {
        return getCurrentActivity();
    }
  
    public static void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
        }
    }
  
    @ReactMethod
    public void takeSnapshot(final int tag, final ReadableMap options, final Promise promise) {
  
        final ReactApplicationContext context = getReactApplicationContext();
        final String format = options.hasKey("format") ? options.getString("format") : "png";
        final Bitmap.CompressFormat compressFormat =
            format.equals(SNAPSHOT_FORMAT_PNG) ? Bitmap.CompressFormat.PNG :
                format.equals(SNAPSHOT_FORMAT_JPG) ? Bitmap.CompressFormat.JPEG : null;
        final double quality = options.hasKey("quality") ? options.getDouble("quality") : 1.0;
        final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        final Integer width =
            options.hasKey("width") ? (int) (displayMetrics.density * options.getDouble("width")) : 0;
        final Integer height =
            options.hasKey("height") ? (int) (displayMetrics.density * options.getDouble("height")) : 0;
        final String result = options.hasKey("result") ? options.getString("result") : "file";
      
        UIManagerModule uiManager = context.getNativeModule(UIManagerModule.class);
        uiManager.addUIBlock(new UIBlock() {
            public void execute(NativeViewHierarchyManager nvhm) {
                AirMapView view = (AirMapView) nvhm.resolveView(tag);
                if (view == null) {
                    promise.reject("AirMapView not found");
                    return;
                }
                if (view.map == null) {
                    promise.reject("AirMapView.map is not valid");
                    return;
                }
                view.map.snapshot(new HuaweiMap.SnapshotReadyCallback() {
                    public void onSnapshotReady(@Nullable Bitmap snapshot) {
                  
                        if (snapshot == null) {
                            promise.reject("Failed to generate bitmap, snapshot = null");
                            return;
                        }
                        if ((width != 0) && (height != 0) &&
                            (width != snapshot.getWidth() || height != snapshot.getHeight())) {
                            snapshot = Bitmap.createScaledBitmap(snapshot, width, height, true);
                        }
                      
                        if (result.equals(SNAPSHOT_RESULT_FILE)) {
                            File tempFile;
                            FileOutputStream outputStream;
                            try {
                                tempFile =
                                    File.createTempFile("AirMapSnapshot", "." + format, context.getCacheDir());
                                outputStream = new FileOutputStream(tempFile);
                            } catch (Exception e) {
                                promise.reject(e);
                                return;
                            }
                            snapshot.compress(compressFormat, (int) (100.0 * quality), outputStream);
                            closeQuietly(outputStream);
                            String uri = Uri.fromFile(tempFile).toString();
                            promise.resolve(uri);
                        } else if (result.equals(SNAPSHOT_RESULT_BASE64)) {
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            snapshot.compress(compressFormat, (int) (100.0 * quality), outputStream);
                            closeQuietly(outputStream);
                            byte[] bytes = outputStream.toByteArray();
                            String data = Base64.encodeToString(bytes, Base64.NO_WRAP);
                            promise.resolve(data);
                        }
                    }
                });
            }
        });
    }
  
    @ReactMethod
    public void getCamera(final int tag, final Promise promise) {
        final ReactApplicationContext context = getReactApplicationContext();
      
        UIManagerModule uiManager = context.getNativeModule(UIManagerModule.class);
        uiManager.addUIBlock(new UIBlock() {
            @Override
            public void execute(NativeViewHierarchyManager nvhm) {
                AirMapView view = (AirMapView) nvhm.resolveView(tag);
                if (view == null) {
                    promise.reject("AirMapView not found");
                    return;
                }
                if (view.map == null) {
                    promise.reject("AirMapView.map is not valid");
                    return;
                }
              
                CameraPosition position = view.map.getCameraPosition();
              
                WritableMap centerJson = new WritableNativeMap();
                centerJson.putDouble("latitude", position.target.latitude);
                centerJson.putDouble("longitude", position.target.longitude);
              
                WritableMap cameraJson = new WritableNativeMap();
                cameraJson.putMap("center", centerJson);
                cameraJson.putDouble("heading", (double)position.bearing);
                cameraJson.putDouble("zoom", (double)position.zoom);
                cameraJson.putDouble("pitch", (double)position.tilt);
              
                promise.resolve(cameraJson);
            }
        });
    }
  
    @ReactMethod
    public void pointForCoordinate(final int tag, ReadableMap coordinate, final Promise promise) {
        final ReactApplicationContext context = getReactApplicationContext();
        final double density = (double) context.getResources().getDisplayMetrics().density;
      
        final LatLng coord = new LatLng(
            coordinate.hasKey("latitude") ? coordinate.getDouble("latitude") : 0.0,
            coordinate.hasKey("longitude") ? coordinate.getDouble("longitude") : 0.0
        );
      
        UIManagerModule uiManager = context.getNativeModule(UIManagerModule.class);
        uiManager.addUIBlock(new UIBlock() {
            @Override
            public void execute(NativeViewHierarchyManager nvhm) {
                AirMapView view = (AirMapView) nvhm.resolveView(tag);
                if (view == null) {
                    promise.reject("AirMapView not found");
                    return;
                }
                if (view.map == null) {
                    promise.reject("AirMapView.map is not valid");
                    return;
                }
               
                Point pt = view.map.getProjection().toScreenLocation(coord);
               
                WritableMap ptJson = new WritableNativeMap();
                ptJson.putDouble("x", (double)pt.x / density);
                ptJson.putDouble("y", (double)pt.y / density);
               
                promise.resolve(ptJson);
            }
        });
    }
  
    @ReactMethod
    public void coordinateForPoint(final int tag, ReadableMap point, final Promise promise) {
        final ReactApplicationContext context = getReactApplicationContext();
        final double density = (double) context.getResources().getDisplayMetrics().density;
      
        final Point pt = new Point(
            point.hasKey("x") ? (int)(point.getDouble("x") * density) : 0,
            point.hasKey("y") ? (int)(point.getDouble("y") * density) : 0
        );
      
        UIManagerModule uiManager = context.getNativeModule(UIManagerModule.class);
        uiManager.addUIBlock(new UIBlock() {
            @Override
            public void execute(NativeViewHierarchyManager nvhm) {
                AirMapView view = (AirMapView) nvhm.resolveView(tag);
                if (view == null) {
                    promise.reject("AirMapView not found");
                    return;
                }
                if (view.map == null) {
                    promise.reject("AirMapView.map is not valid");
                    return;
                }
              
                LatLng coord = view.map.getProjection().fromScreenLocation(pt);
              
                WritableMap coordJson = new WritableNativeMap();
                coordJson.putDouble("latitude", coord.latitude);
                coordJson.putDouble("longitude", coord.longitude);
              
                promise.resolve(coordJson);
            }
        });
    }
  
    @ReactMethod
    public void getMapBoundaries(final int tag, final Promise promise) {
        final ReactApplicationContext context = getReactApplicationContext();
      
        UIManagerModule uiManager = context.getNativeModule(UIManagerModule.class);
        uiManager.addUIBlock(new UIBlock() {
            @Override
            public void execute(NativeViewHierarchyManager nvhm) {
                AirMapView view = (AirMapView) nvhm.resolveView(tag);
                if (view == null) {
                    promise.reject("AirMapView not found");
                    return;
                }
                if (view.map == null) {
                    promise.reject("AirMapView.map is not valid");
                    return;
                }
              
                double[][] boundaries = view.getMapBoundaries();
              
                WritableMap coordinates = new WritableNativeMap();
                WritableMap northEastHash = new WritableNativeMap();
                WritableMap southWestHash = new WritableNativeMap();
              
                northEastHash.putDouble("longitude", boundaries[0][0]);
                northEastHash.putDouble("latitude", boundaries[0][1]);
                southWestHash.putDouble("longitude", boundaries[1][0]);
                southWestHash.putDouble("latitude", boundaries[1][1]);
              
                coordinates.putMap("northEast", northEastHash);
                coordinates.putMap("southWest", southWestHash);
              
                promise.resolve(coordinates);
            }
        });
    }
}
