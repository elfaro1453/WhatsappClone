package kuy.belajar.whatsappclone.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.fragment_chat.view.*
import kuy.belajar.whatsappclone.R
import kuy.belajar.whatsappclone.model.ChatList
import kuy.belajar.whatsappclone.model.User
import kuy.belajar.whatsappclone.recyclerview.UserSearchItemAdapter

class ChatFragment : Fragment() {
    private val dbRef = FirebaseDatabase.getInstance().reference
    private val userRef = FirebaseAuth.getInstance().currentUser
    private lateinit var userListRef: DatabaseReference
    private lateinit var userListListener: ValueEventListener
    private val userList = arrayListOf<ChatList>()
    private lateinit var userInfoRef: DatabaseReference
    private lateinit var userInfoListener: ValueEventListener
    private val userInfoList = arrayListOf<User>()
    private lateinit var rvAdapter : UserSearchItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_chat, container, false)
        rvAdapter = UserSearchItemAdapter()
        view.rv_chat.run {
            layoutManager = LinearLayoutManager(view.context)
            setHasFixedSize(true)
            adapter = rvAdapter
        }
        userListListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                if (snapshot.hasChildren()) {
                    snapshot.children.forEach {
                        val userID = it.getValue(ChatList::class.java) as ChatList
                        userList.add(userID)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        userInfoListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userInfoList.clear()
                if (snapshot.hasChildren()) {
                    snapshot.children.forEach { data ->
                        val user = data.getValue(User::class.java) as User
                        userList.forEach { chatUser ->
                            if(user.uid == chatUser.id) userInfoList.add(user)
                        }
                    }
                }
                rvAdapter.addUser(userInfoList, true)
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        userListRef = dbRef.child("ChatList").child(userRef?.uid.toString())
        userListRef.addValueEventListener(userListListener)

        userInfoRef = dbRef.child("Users")
        userInfoRef.addValueEventListener(userInfoListener)

        return view
    }

    override fun onDestroy() {

        super.onDestroy()
    }
}