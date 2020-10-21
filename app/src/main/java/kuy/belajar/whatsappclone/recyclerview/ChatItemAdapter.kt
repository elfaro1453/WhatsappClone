package kuy.belajar.whatsappclone.recyclerview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kuy.belajar.whatsappclone.R
import kuy.belajar.whatsappclone.model.Chat

/**
 * Created by Imam Fahrur Rofi on 20/10/20.
 */
class ChatItemAdapter(val receiverID: String) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val arrayChat = arrayListOf<Chat>()
    private var imageUrl = ""

    fun addChats(chats: List<Chat>, receiverImage: String) {
        arrayChat.clear()
        arrayChat.addAll(chats)
        imageUrl = receiverImage
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 1) {
            val view = inflater.inflate(R.layout.chat_item_right, parent, false)
            ChatItemRightVH(view)
        } else {
            val view = inflater.inflate(R.layout.chat_item_left, parent, false)
            ChatItemLeftVH(view)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val chat = arrayChat[position]
        return if (chat.sender == receiverID) 0 else 1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chat = arrayChat[position]
        val viewType = getItemViewType(position)
        if (viewType == 1) (holder as ChatItemRightVH).bind(chat) else (holder as ChatItemLeftVH).bind(
            chat,
            imageUrl
        )
    }

    override fun getItemCount(): Int = arrayChat.size
}