package com.example.domain.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

object ShareManager {

    fun getFileUri(context: Context, file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    /**
     * Presents the Android Universal Share Sheet for a single file.
     */
    fun shareFile(
        context: Context,
        file: File,
        mimeType: String = "application/pdf",
        chooserTitle: String = "Share via MTE Merge"
    ) {
        try {
            val uri = getFileUri(context, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Presents the Android Universal Share Sheet for multiple files.
     */
    fun shareMultipleFiles(
        context: Context,
        files: List<File>,
        mimeType: String = "application/pdf",
        chooserTitle: String = "Share Documents"
    ) {
        try {
            val uris = ArrayList<Uri>()
            for (f in files) {
                uris.add(getFileUri(context, f))
            }
            shareUris(context, uris, mimeType, chooserTitle)
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing files: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Presents the Android Universal Share Sheet for multiple file URIs.
     */
    fun shareUris(
        context: Context,
        uris: List<Uri>,
        mimeType: String = "application/pdf",
        chooserTitle: String = "Share Documents"
    ) {
        try {
            val arrayList = ArrayList<Uri>(uris)
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayList)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing files: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens the file in an installed viewer app (e.g. Google PDF Viewer, Drive, Photos).
     */
    fun openFile(context: Context, file: File, mimeType: String = "application/pdf") {
        try {
            val uri = getFileUri(context, file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Saves the generated file to the public Downloads folder using MediaStore.
     */
    suspend fun saveToDownloads(
        context: Context,
        file: File,
        targetFileName: String,
        mimeType: String = "application/pdf"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, targetFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MTE_Merge")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val collectionUri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = context.contentResolver.insert(collectionUri, contentValues) ?: return@withContext false

                context.contentResolver.openOutputStream(itemUri)?.use { outStream ->
                    FileInputStream(file).use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(itemUri, contentValues, null, null)
                true
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(downloadsDir, "MTE_Merge").apply { mkdirs() }
                val destFile = File(targetDir, targetFileName)
                file.copyTo(destFile, overwrite = true)
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}
