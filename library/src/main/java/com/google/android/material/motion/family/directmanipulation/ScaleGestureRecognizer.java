/*
 * Copyright 2016-present The Material Motion Authors. All Rights Reserved.
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
package com.google.android.material.motion.family.directmanipulation;

import android.graphics.PointF;
import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;

/**
 * A gesture recognizer that generates scale events.
 */
public class ScaleGestureRecognizer extends GestureRecognizer {
  private float currentCentroidX;
  private float currentCentroidY;

  private float initialSpan;
  private float currentSpan;

  public boolean onTouchEvent(MotionEvent event) {
    PointF centroid = calculateCentroid(event);
    float centroidX = centroid.x;
    float centroidY = centroid.y;
    float span = calculateAverageSpan(event, centroidX, centroidY);

    int action = MotionEventCompat.getActionMasked(event);
    int pointerCount = event.getPointerCount();
    if (action == MotionEvent.ACTION_DOWN
      || action == MotionEvent.ACTION_POINTER_DOWN && pointerCount == 2) {
      currentCentroidX = centroidX;
      currentCentroidY = centroidY;

      initialSpan = span;
      currentSpan = span;
    }
    if (action == MotionEvent.ACTION_POINTER_DOWN && pointerCount > 2
      || action == MotionEvent.ACTION_POINTER_UP && pointerCount > 2) {
      float adjustX = centroidX - currentCentroidX;
      float adjustY = centroidY - currentCentroidY;

      currentCentroidX += adjustX;
      currentCentroidY += adjustY;

      float adjustSpan = span / currentSpan;

      initialSpan *= adjustSpan;
      currentSpan *= adjustSpan;
    }
    if (action == MotionEvent.ACTION_MOVE && pointerCount < 2) {
      currentCentroidX = centroidX;
      currentCentroidY = centroidY;
    }
    if (action == MotionEvent.ACTION_MOVE && pointerCount >= 2) {
      currentCentroidX = centroidX;
      currentCentroidY = centroidY;

      if (!isInProgress()) {
        float deltaSpan = span - initialSpan;
        if (Math.abs(deltaSpan) > scaleSlop) {
          float adjustSpan = 1 + Math.signum(deltaSpan) * (scaleSlop / initialSpan);

          initialSpan *= adjustSpan;
          currentSpan *= adjustSpan;

          setState(BEGAN);
        }
      }

      if (isInProgress()) {
        currentSpan = span;

        setState(CHANGED);
      }
    }
    if (action == MotionEvent.ACTION_POINTER_UP && pointerCount == 2
      || action == MotionEvent.ACTION_CANCEL) {
      if (isInProgress()) {
        currentCentroidX = centroidX;
        currentCentroidY = centroidY;

        initialSpan = 0;
        currentSpan = 0;

        if (action == MotionEvent.ACTION_POINTER_UP) {
          setState(RECOGNIZED);
        } else {
          setState(CANCELLED);
        }
      }
    }

    return true;
  }

  /**
   * Returns the scale of the pinch gesture.
   * <p>
   * This reports the total scale over time since the {@link #BEGAN beginning} of the gesture.
   * This is not a delta value from the last {@link #CHANGED update}.
   */
  public float getScale() {
    return initialSpan > 0 ? currentSpan / initialSpan : 1;
  }

  @Override
  public float getCentroidX() {
    return currentCentroidX;
  }

  @Override
  public float getCentroidY() {
    return currentCentroidY;
  }

  /**
   * Calculates the average span of all the active pointers in the given motion event.
   * <p>
   * The average span is twice the average distance of all pointers to the given centroid.
   */
  private float calculateAverageSpan(MotionEvent event, float centroidX, float centroidY) {
    int action = MotionEventCompat.getActionMasked(event);
    int index = MotionEventCompat.getActionIndex(event);

    float sum = 0;
    int num = 0;
    for (int i = 0, count = event.getPointerCount(); i < count; i++) {
      if (action == MotionEvent.ACTION_POINTER_UP && index == i) {
        continue;
      }

      sum += calculateDistance(event, i, centroidX, centroidY);
      num++;
    }

    float averageDistance = sum / num;
    return averageDistance * 2;
  }

  /**
   * Calculates the distance between the pointer given by the pointer index and the given centroid.
   */
  private float calculateDistance(
    MotionEvent event, int pointerIndex, float centroidX, float centroidY) {
    PointF rawPoint = calculateRawPoint(event, pointerIndex);
    float distanceX = rawPoint.x - centroidX;
    float distanceY = rawPoint.y - centroidY;

    return (float) Math.sqrt(distanceX * distanceX + distanceY * distanceY);
  }
}
