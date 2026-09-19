package org.dergigi.fishyfishy

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File
import java.io.InputStream
import java.util.UUID

private const val PREFIX = "swim-"
private fun isRevision(name: String) = name.startsWith(PREFIX) && name.endsWith(".json")
private fun InputStream.readRevision(): SwimRevision = use {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        require(output.size() + count <= 262144) { "A swim file is too large." }
        output.write(buffer, 0, count)
    }
    RevisionCodec.decode(output.toString("UTF-8"))
}

class LocalRevisionStore(private val directory: File) : RevisionStore {
    init { require(directory.isDirectory || directory.mkdirs()) { "Cannot create journal directory." } }
    override fun read(): List<SwimRevision> = requireNotNull(directory.listFiles()) { "Journal directory is unavailable." }
        .filter { it.isFile && isRevision(it.name) }.map { it.inputStream().readRevision() }
    override fun append(revision: SwimRevision) {
        val target = File(directory, "$PREFIX${revision.id}.json")
        if (target.exists()) {
            require(target.inputStream().readRevision() == revision) { "Revision ID collision." }; return
        }
        val pending = File(directory, ".pending-${UUID.randomUUID()}")
        try {
            pending.outputStream().use { out -> out.write(RevisionCodec.encode(revision).toByteArray()); out.fd.sync() }
            require(pending.renameTo(target)) { "Couldn't commit swim file." }
        } finally { pending.delete() }
    }
}

/** SAF grants access only to the folder the parent chose, without broad storage permission. */
class FolderRevisionStore(private val resolver: ContentResolver, val tree: Uri) : RevisionStore {
    private val root = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
    private data class Entry(val name: String, val uri: Uri, val mime: String)
    private fun children(): List<Entry> {
        displayName() // A missing root must not look like an empty journal.
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE)
        return requireNotNull(resolver.query(uri, projection, null, null, null)) { "Folder is unavailable." }.use { cursor ->
            val result = mutableListOf<Entry>()
            while (cursor.moveToNext()) result.add(Entry(cursor.getString(1), DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0)), cursor.getString(2)))
            result
        }
    }
    fun displayName(): String = requireNotNull(resolver.query(root, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)).use {
        require(it.moveToFirst()); it.getString(0)
    }
    fun verifyWritable() {
        var probe = requireNotNull(DocumentsContract.createDocument(resolver, root, "application/octet-stream", ".pending-${UUID.randomUUID()}")) { "Cannot write to this folder." }
        try {
            requireNotNull(resolver.openOutputStream(probe, "wt")).use { it.write("FishyFishy".toByteArray()) }
            probe = requireNotNull(DocumentsContract.renameDocument(resolver, probe, ".pending-${UUID.randomUUID()}")) { "Folder cannot commit files." }
            require(requireNotNull(resolver.openInputStream(probe)).bufferedReader().use { it.readText() } == "FishyFishy")
        } finally { DocumentsContract.deleteDocument(resolver, probe) }
    }
    override fun read(): List<SwimRevision> = children().filter { isRevision(it.name) && it.mime != DocumentsContract.Document.MIME_TYPE_DIR }
        .map { requireNotNull(resolver.openInputStream(it.uri)).readRevision() }
    override fun append(revision: SwimRevision) {
        val name = "$PREFIX${revision.id}.json"
        children().firstOrNull { it.name == name }?.let { existing ->
            require(requireNotNull(resolver.openInputStream(existing.uri)).readRevision() == revision) { "Revision ID collision." }
            return
        }
        // Sync tools can see the directory while we write. Only closed, validated JSON is renamed into view.
        val pending = requireNotNull(DocumentsContract.createDocument(resolver, root, "application/octet-stream", ".pending-${UUID.randomUUID()}")) { "Cannot write to this folder." }
        var committed = false
        try {
            requireNotNull(resolver.openOutputStream(pending, "wt")).use { it.write(RevisionCodec.encode(revision).toByteArray()) }
            require(requireNotNull(resolver.openInputStream(pending)).readRevision() == revision)
            val final = requireNotNull(DocumentsContract.renameDocument(resolver, pending, name)) { "This folder doesn't support committing swim files." }
            committed = true
            require(requireNotNull(resolver.openInputStream(final)).readRevision() == revision) { "Swim file verification failed." }
        } finally {
            if (!committed) runCatching { DocumentsContract.deleteDocument(resolver, pending) }
        }
    }
}
