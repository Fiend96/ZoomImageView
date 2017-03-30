# ZoomImageView
#### 一、简介

​	一个支持手势拖拽、缩放的ImageView。

​	手势操作是智能手机的一大特色，特别是多点手势有着良好的交互体验。手势操纵图片广泛应用于Android的各大app，包括QQ、微信和微博等。本项目正是基于上述app的体验作为参考而开发的一个控件。其可以支持拖动、点击、双点滑动等手势操纵图片的变换。

#### 二、主要实现技术

* OnTouchListener触摸事件：处理多点触摸操作


* GestureDetector手势辅助类：处理滑动、单击和快速滑动(滑动后有惯性)事件

* Matrix变化：实现图片的缩放和移动效果，需要设置

  `super.setScaleType(ScaleType.MATRIX);`

  然后通过下述几种变化实现缩放和移动效果：

  ```
  postScale(scaleX, scaleY);//改变大小
  ```

  ```
  postTranslate(tranX, tranY);//改变位置
  ```

  ```
  setImageMatrix(matrix);//ImageView的方法，设置当前图片的矩阵
  ```

* ValueAnimator实现动画效果，有以下情况需要动画

  - 缩放倍数小于正常倍数时需要恢复正常倍数
  - 拖拽时超过了边界时需要恢复与边界重合

* 详细实现请看代码

#### 三、实现细节

##### 1、操作模式

​	判断处于那个状态而进行不同的操作

```
int NONE = 0;//无法操作
int DRAG = 1;//拖动模式，在放大的情况下
int ZOOM = 2;//缩放模式
int DOUBLECLICK = 4;//双击模式
```

##### 2、大小模式

​	需要判断不同的大小来决定操作模式

```
int ENLARGE = 5;//放大模式
int NARROW = 6;//缩小模式
float MAX_SCALE = 4f;//最大放大倍数
float MIN_SCALE = 0.5f;//最小缩放倍数
```

##### 3、拖动

​	判断是否可以进入拖拽模式

```
if (sizeMode == ENLARGE || ableTranY) {// 放大或Y轴可拖动
    mode = DRAG;// 单点进入拖动模式
    startPoint.set(e.getX(), e.getY());//保存开始拖动时点的数据
    lastPoint.set(e.getX(), e.getY());
}
```

​	在GestureDetector的onSroll方法中处理拖动效果

```
if (mode == DRAG) {// 拖动模式
    mCurrentMatrix.set(mSaveMatrix);//每次拖动都是基于开始拖动时候的参数来计算的
    dragImage(e2);//处理拖动
}
```

​	拖动的具体操作

```
float currentX = event.getX();// 获取当前的x坐标
float currentY = event.getY();// 获取当前的Y坐标
float tranX = currentX - startPoint.x;// 移动的x轴距离
float tranY = currentY - startPoint.y;// 移动的y轴距离
mCurrentMatrix.postTranslate(tranX, tranY);
setImageMatrix(mCurrentMatrix);
```

##### 4、缩放

​	判断是否可以进入缩放状态

```
case MotionEvent.ACTION_POINTER_DOWN: //第二次down事件，表示两点同时触摸
    getSizeMode();
    oldDistance = spacing(event); //计算两点的距离
    if (oldDistance > 10f) { //两点超过10才可以拖动
        mode = ZOOM;
    }
```

​	在OnTouchListener中进行缩放操作

```
case MotionEvent.ACTION_MOVE:
    if (mode == ZOOM) {// 缩放模式
        mCurrentMatrix.set(mSaveMatrix);//每次缩放都是基于开始缩放时候的参数来计算的
        zoomImage(event);
    }
```

​	缩放的具体操作

```
float newDistance = spacing(event);// 获取新的距离
float scale = newDistance / oldDistance;// 缩放比例
scale = getScaleType(scale);// 获取缩放的类型
midPoint(midPoint, event);//计算中点位置，根据中点位置来进行缩放
mCurrentMatrix.postScale(scale, scale, midPoint.x, midPoint.y);
setImageMatrix(mCurrentMatrix);
```

##### 5、恢复动画

​	在缩放倍数小于正常倍数(初始化的倍数)时需要恢复到正常倍数。在拖拽图片超出了边界也需要恢复到不超出边界。显然，只有在手指离开屏幕的时候才会需要恢复，所以需要监控UP事件来判断是否需要恢复

```
case MotionEvent.ACTION_UP:
    // 拖拽模式
    if (mode == DRAG) {
        after();
        mode = NONE;
        // 缩放模式
    } else if (mode == ZOOM) {
        after();
        mode = NONE;
    } else if (mode == DOUBLECLICK) {
        mode = NONE;
    }
```

​	恢复正常状态的操作就是判断倍数是否小于正常倍数

```
if (mScale < getNolmalScale()) {
  //处理
}
```

​	和是否拖出边界

```
if (mTranX > 0) { //拖出左边界
	//处理
} else if (Math.abs(mTranX) > imageWidth - mViewWidth) {//拖出右边界
	//处理
}

// 对Y轴移动处理，若图片高度小于控件高度则无法进行Y轴拖动(图片宽度一定大于或等于控件宽度)
if (imageHeight > mViewHeight) {
  if (mTranY > 0) {// 拖出上边界
      //处理
  } else if (Math.abs(mTranY) > imageHeight - mViewHeight) { //拖出下边界
      //处理
  }
} else if (imageHeight < mViewHeight) { //无法进行拖动
  	//处理
}
```



##### 6、恢复初始化状态

​	默认双击恢复初始化状态，也可以调用方法来恢复初始化状态。具体计算与拖拽缩放类似，这里不做更多解释，详细请看代码。

#### 四、解决与ViewPager的滑动冲突

​	在ViewPager中使用该控件需要判断什么时候ViewPager需要处理事件(翻页)。这里是在拖动滑出边界时将事件交给ViewPager来处理，使用的接口为

```
//isInterrupt true时父View不拦截事件，false时View可以拦截事件
parent.requestDisallowInterceptTouchEvent(isInterrupt);
```

​	因为事件需要从父布局中传入，所以必须在DOWN事件(父布局无法拦截DOWN事件)中通知父布局不拦截事件

```
setInterrupt(true);
if (sizeMode == NONE && !ableTranY) { 
	// 判断sizeMode是因为初始化状态只能进行缩放操作
	// ableTranY表示Y轴可拖动，因为初始化时图片宽度于控件宽度相同，所以X轴无法拖动，若图片Y轴可拖动同样可进行拖动操作
    setInterrupt(false);
}
```

​	在拖动过程中判断是否将事件交给父布局

```
//isInterruptLeft和isInterruptRight为可设置的参数，表示强制拦截
boolean ableToTurnLeft = mTranX > 3 && isInterruptLeft;
boolean ableToTurnRight = Math.abs(mTranX) > Math.abs(mImageWidth - mViewWidth) + 3 && isInterruptRight;
//将滑动控制权返回给父view同时调整位置
if (ableToTurnLeft) {
    values[Matrix.MTRANS_X] = 0; //将图片恢复到不超过边界的状态
    setInterrupt(false);
    mCurrentMatrix.setValues(values);
} else if (ableToTurnRight) {
    values[Matrix.MTRANS_X] = mViewWidth - mImageWidth;//将图片恢复到不超过边界的状态
    mCurrentMatrix.setValues(values);
    setInterrupt(false);
} 
```



#### 五、参考

* QQ、微信和微博的图片控件
* PhotoView https://github.com/chrisbanes/PhotoView
* 《Android开发艺术探索》的滑动事件讲解