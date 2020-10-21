package kuy.belajar.whatsappclone

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_message_chat.*
import kuy.belajar.whatsappclone.model.Chat
import kuy.belajar.whatsappclone.model.User
import kuy.belajar.whatsappclone.recyclerview.ChatItemAdapter

class MessageChatActivity : AppCompatActivity() {
    companion object {
        const val SEND_IMAGE = "RANDOMSTRING-OR_WHATEVER"
        const val IS_SEEN_FALSE = "false"
    }

    private lateinit var receiverID: String
    private lateinit var senderID: String
    private lateinit var dbRef: DatabaseReference
    private lateinit var userRef: DatabaseReference
    private lateinit var storageRef: StorageReference
    private lateinit var chatsRef: DatabaseReference
    private lateinit var chatSenderRef: DatabaseReference
    private lateinit var chatReceiverRef: DatabaseReference
    private lateinit var userListener: ValueEventListener
    private lateinit var chatSenderListener: ValueEventListener
    private lateinit var chatListListener: ValueEventListener
    private lateinit var chatAdapter: ChatItemAdapter
    private val requestCodeActivity = 438
    private var firebaseUser: FirebaseUser? = null
    private var receiverImage = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message_chat)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        receiverID = intent.getStringExtra("visit_id") as String
        chatAdapter = ChatItemAdapter(receiverID)
        firebaseUser = FirebaseAuth.getInstance().currentUser
        senderID = firebaseUser?.uid as String
        dbRef = FirebaseDatabase.getInstance().reference
        userRef = dbRef.child("Users").child(senderID)
        storageRef = FirebaseStorage.getInstance().reference.child("Chat Images")

        userListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val user = snapshot.getValue(User::class.java) as User
                    user_name.text = user.username
                    if (user.profile.isNotBlank()) {
                        receiverImage = user.profile
                        Picasso.get().load(user.profile)
                            .placeholder(R.drawable.ic_profile).into(profile_image)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        chatSenderListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    chatSenderRef.child("id").setValue(receiverID)
                }
                chatReceiverRef.child("id").setValue(senderID)
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        chatListListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    if (snapshot.hasChildren()) {
                        val chatList = arrayListOf<Chat>()
                        snapshot.children.forEach {
                            val chat = it.getValue(Chat::class.java) as Chat
                            if (chat.receiver == senderID && chat.sender == receiverID
                                || chat.receiver == receiverID && chat.sender == senderID
                            ) {
                                chatList.add(chat)
                            }
                            chatAdapter.addChats(chatList, receiverImage)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        firebaseUser?.let {
            senderID = it.uid

            chatSenderRef = FirebaseDatabase.getInstance().reference
                .child("ChatList")
                .child(senderID)
                .child(receiverID)

            chatReceiverRef = FirebaseDatabase.getInstance().reference
                .child("ChatList")
                .child(receiverID)
                .child(senderID)
        }

        send_message.setOnClickListener {
            input_text_message.run {
                val message = this.text.toString()
                if (message.isNotBlank()) {
                    sendMessageToUser(message)
                }
                text.clear()
                clearFocus()
            }
        }
        attach_image.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(
                Intent.createChooser(intent, "Pick Image"),
                requestCodeActivity
            )
        }

        rv_chat.run {
            adapter = chatAdapter
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@MessageChatActivity)
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                // Wait till recycler_view will update itself and then scroll to the end.
                post {
                    adapter?.itemCount?.takeIf { it > 0 }?.let {
                        scrollToPosition(it - 1)
                    }
                }
            }
        }

        val receiverRef = dbRef.child("Users").child(receiverID)
        receiverRef.addListenerForSingleValueEvent(userListener)

        chatsRef = FirebaseDatabase.getInstance().reference.child("Chats")
        chatsRef.addValueEventListener(chatListListener)
    }

    override fun onStart() {
        super.onStart()
        userRef.addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val usr = snapshot.getValue(User::class.java) as User
                    val userUpdate = usr.copy(status = "online")
                    userRef.setValue(userUpdate)
                }

                override fun onCancelled(error: DatabaseError) {}
            }
        )
    }

    override fun onStop() {
        super.onStop()
        userRef.addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val usr = snapshot.getValue(User::class.java) as User
                    val userUpdate = usr.copy(status = "offline")
                    userRef.child("Users").child(senderID).setValue(userUpdate)
                }

                override fun onCancelled(error: DatabaseError) {}
            }
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestCodeActivity && resultCode == Activity.RESULT_OK && data?.data !== null) {
            data.data?.let { fileUri ->
                val loadingBar = ProgressDialog(this)
                loadingBar.setMessage("Please Wait..\nSending Image..")
                loadingBar.show()

                val messageId = dbRef.push().key.toString()
                val filePath = storageRef.child("$messageId.jpg")
                val uploadTask = filePath.putFile(fileUri)
                uploadTask.continueWithTask {
                    if (!it.isSuccessful) {
                        it.exception?.let { error ->
                            throw error
                        }
                    }
                    return@continueWithTask filePath.downloadUrl
                }.addOnCompleteListener { taskUpload ->
                    if (taskUpload.isSuccessful) {
                        val downloadUrl = taskUpload.result.toString()
                        val chat = Chat(
                            sender = senderID,
                            receiver = receiverID,
                            message = SEND_IMAGE,
                            isSeen = IS_SEEN_FALSE,
                            url = downloadUrl,
                            messageID = messageId
                        )
                        dbRef.child("Chats").child(messageId).setValue(chat)
                            .addOnCompleteListener {
                                if (it.isSuccessful) {
                                    chatSenderRef.addListenerForSingleValueEvent(chatSenderListener)
                                }
                            }
                    }
                    loadingBar.dismiss()
                }
            }
        }
    }

    private fun sendMessageToUser(message: String) {
        val messageKey = dbRef.push().key
        messageKey?.let {
            val chat = Chat(
                sender = senderID,
                receiver = receiverID,
                message = message,
                isSeen = IS_SEEN_FALSE,
                url = "",
                messageID = it
            )
            dbRef.child("Chats").child(it).setValue(chat)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        chatSenderRef.addListenerForSingleValueEvent(chatSenderListener)
                    }
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        chatsRef.removeEventListener(chatListListener)
    }
}