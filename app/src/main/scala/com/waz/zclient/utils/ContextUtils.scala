/**
 * Wire
 * Copyright (C) 2018 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.waz.zclient.utils

import android.content.res.{Configuration, Resources, TypedArray}
import android.content.{Context, DialogInterface, Intent}
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.support.annotation.StyleableRes
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.{AttributeSet, DisplayMetrics, TypedValue}
import android.view.WindowManager
import android.widget.Toast
import com.waz.utils.returning
import com.waz.zclient.R
import com.waz.zclient.appentry.DialogErrorMessage
import com.waz.zclient.ui.utils.ResourceUtils

import scala.concurrent.{Future, Promise}
import scala.util.Success


object ContextUtils {
  def getColor(resId: Int)(implicit context: Context): Int = ContextCompat.getColor(context, resId)

  def getColorWithTheme(resId: Int, context: Context): Int =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) getColor(resId)(context)
    else context.getResources.getColor(resId, context.getTheme)

  def getColorStateList(resId: Int)(implicit context: Context) = ContextCompat.getColorStateList(context, resId)

  def getInt(resId: Int)(implicit context: Context) = context.getResources.getInteger(resId)

  def getString(resId: Int)(implicit context: Context): String = context.getResources.getString(resId)
  def getString(resId: Int, args: String*)(implicit context: Context): String = context.getResources.getString(resId, args:_*)

  def showToast(resId: Int, long: Boolean = true)(implicit context: Context): Unit =
    Toast.makeText(context, resId, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()

  def showToast(msg: String)(implicit context: Context): Unit =
    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

  def getStringOrEmpty(resId: Int)(implicit context: Context): String = if (resId > 0) getString(resId) else ""
  def getStringOrEmpty(resId: Int, args: String*)(implicit context: Context): String = if (resId > 0) getString(resId, args:_*) else ""

  def getQuantityString(resId: Int, quantity: Int, args: AnyRef*)(implicit context: Context): String = context.getResources.getQuantityString(resId, quantity, args:_*)

  def getDimenPx(resId: Int)(implicit context: Context) = context.getResources.getDimensionPixelSize(resId)
  def getDimen(resId: Int)(implicit context: Context) = context.getResources.getDimension(resId)

  def getDrawable(resId: Int, theme: Option[Resources#Theme] = None)(implicit context: Context): Drawable = {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      DeprecationUtils.getDrawable(context, resId)
    } else
      context.getResources.getDrawable(resId, theme.orNull)
  }

  def getIntArray(resId: Int)(implicit context: Context) = context.getResources.getIntArray(resId)
  def getStringArray(resId: Int)(implicit context: Context) = context.getResources.getStringArray(resId)
  def getResEntryName(resId: Int)(implicit context: Context) = context.getResources.getResourceEntryName(resId)

  def getResourceFloat(resId: Int)(implicit context: Context) = ResourceUtils.getResourceFloat(context.getResources, resId)

  def toPx(dp: Int)(implicit context: Context) = (dp * context.getResources.getDisplayMetrics.density).toInt

  def getLocale(implicit context: Context) = {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
      DeprecationUtils.getDefaultLocale(context)
    } else {
      context.getResources.getConfiguration.getLocales.get(0)
    }
  }

  def withStyledAttributes[A](set: AttributeSet, @StyleableRes attrs: Array[Int])(body: TypedArray => A)(implicit context: Context) = {
    val a = context.getTheme.obtainStyledAttributes(set, attrs, 0, 0)
    try body(a) finally a.recycle()
  }

  def getStyledColor(resId: Int)(implicit context: Context): Int = {
    val typedValue  = new TypedValue
    val a  = context.obtainStyledAttributes(typedValue.data, Array[Int](resId))
    val color = a.getColor(0, 0)
    a.recycle()
    color
  }

  /**
    * @return the amount of pixels of the horizontal axis of the phone
    */
  def getOrientationDependentDisplayWidth(implicit context: Context): Int = context.getResources.getDisplayMetrics.widthPixels

  /**
    * @return everytime the amount of pixels of the (in portrait) horizontal axis of the phone
    */
  def getOrientationIndependentDisplayWidth(implicit context: Context): Int =
    if (isInPortrait) context.getResources.getDisplayMetrics.widthPixels
    else context.getResources.getDisplayMetrics.heightPixels

  /**
    * @return the amount of pixels of the vertical axis of the phone
    */
  def getOrientationDependentDisplayHeight(implicit context: Context): Int = context.getResources.getDisplayMetrics.heightPixels

  /**
    * @return everytime the amount of pixels of the (in portrait) width axis of the phone
    */
  def getOrientationIndependentDisplayHeight(implicit context: Context): Int =
    if (isInPortrait) context.getResources.getDisplayMetrics.heightPixels
    else context.getResources.getDisplayMetrics.widthPixels

  def getStatusBarHeight(implicit context: Context): Int = getDimensionPixelSize("status_bar_height")

  def getNavigationBarHeight(implicit context: Context): Int =
    getDimensionPixelSize(if (isInPortrait) "navigation_bar_height" else "navigation_bar_height_landscape")

  private def getDimensionPixelSize(name: String)(implicit context: Context): Int = {
    val resourceId = context.getResources.getIdentifier(name, "dimen", "android")
    if (resourceId > 0) context.getResources.getDimensionPixelSize(resourceId) else 0
  }

  def getRealDisplayWidth(implicit context: Context): Int = {
    val realMetrics = new DisplayMetrics
    context.getSystemService(Context.WINDOW_SERVICE).asInstanceOf[WindowManager].getDefaultDisplay.getRealMetrics(realMetrics)
    realMetrics.widthPixels
  }

  def isInLandscape(implicit context: Context): Boolean = isInLandscape(context.getResources.getConfiguration)
  def isInLandscape(configuration: Configuration): Boolean = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
  def isInPortrait(implicit context: Context): Boolean = isInPortrait(context.getResources.getConfiguration)
  def isInPortrait(configuration: Configuration): Boolean = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

  def showGenericErrorDialog()(implicit context: Context): Future[Unit] =
    showErrorDialog(R.string.generic_error_header, R.string.generic_error_message)

  def showErrorDialog(dialogErrorMessage: DialogErrorMessage)(implicit context: Context): Future[Unit] =
    showErrorDialog(dialogErrorMessage.headerResource, dialogErrorMessage.bodyResource)

  //INFORMATION ABOUT DIALOGS: https://developer.android.com/guide/topics/ui/dialogs
  def showErrorDialog(headerRes: Int, msgRes: Int)(implicit context: Context): Future[Unit] = {
    val p = Promise[Unit]()
    val dialog = new AlertDialog.Builder(context)
      .setCancelable(false)
      .setTitle(headerRes)
      .setMessage(msgRes)
      .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
        def onClick(dialog: DialogInterface, which: Int): Unit = {
          dialog.dismiss()
          p.complete(Success({}))
        }
      }).create
    dialog.show()
    p.future
  }

  def showConfirmationDialog(title:    String,
                             msg:      String,
                             positiveRes: Int = android.R.string.ok,
                             negativeRes: Int = android.R.string.cancel)
                            (implicit context: Context): Future[Boolean] = {
    val p = Promise[Boolean]()
    val dialog = new AlertDialog.Builder(context)
      .setTitle(title)
      .setMessage(msg)
      .setPositiveButton(positiveRes, new DialogInterface.OnClickListener {
        override def onClick(dialog: DialogInterface, which: Int) = p.complete(Success(true))
      })
      .setNegativeButton(negativeRes, new DialogInterface.OnClickListener() {
        def onClick(dialog: DialogInterface, which: Int): Unit = dialog.cancel()
      })
      .setOnCancelListener(new DialogInterface.OnCancelListener {
        override def onCancel(dialog: DialogInterface) = p.complete(Success(false))
      })
      .create
    dialog.show()
    p.future
  }

  def showPermissionsErrorDialog(titleRes: Int, msgRes: Int, ackRes: Int = android.R.string.ok)(implicit cxt: Context) = {
    val p = Promise[Unit]()
    val dialog = new AlertDialog.Builder(cxt)
      .setTitle(titleRes)
      .setMessage(msgRes)
      .setPositiveButton(ackRes, null)
      .setNegativeButton(R.string.permissions_denied_dialog_settings, new DialogInterface.OnClickListener() {
        def onClick(dialog: DialogInterface, which: Int): Unit =
          cxt.startActivity(returning(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", cxt.getPackageName, null)))(_.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)))
      })
      .setOnDismissListener(new DialogInterface.OnDismissListener { //From the docs: The system calls onDismiss() upon each event that invokes the onCancel() callback
        override def onDismiss(dialog: DialogInterface) = p.complete(Success({}))
      })
      .create
  }

  //TODO come up with a tidier way of doing this.
  def showConfirmationDialogWithNeutralButton(title:          Int,
                                              msg:            Int,
                                              neutralRes:     Int,
                                              positiveRes:    Int = android.R.string.ok,
                                              negativeRes:    Int = android.R.string.cancel)
                                             (implicit context: Context): Future[Option[Boolean]] = {
    val p = Promise[Option[Boolean]]()
    val dialog = new AlertDialog.Builder(context)
      .setTitle(title)
      .setMessage(msg)
      .setPositiveButton(positiveRes, new DialogInterface.OnClickListener {
        override def onClick(dialog: DialogInterface, which: Int) = p.trySuccess(Some(true))
      })
      .setNegativeButton(negativeRes, new DialogInterface.OnClickListener() {
        def onClick(dialog: DialogInterface, which: Int): Unit = dialog.cancel()
      })
      .setNeutralButton(neutralRes, new DialogInterface.OnClickListener {
        override def onClick(dialog: DialogInterface, which: Int) = p.trySuccess(None)
      })
      .setOnCancelListener(new DialogInterface.OnCancelListener {
        override def onCancel(dialog: DialogInterface) = p.trySuccess(Some(false))
      })
      .create
    dialog.show()
    p.future
  }
}
