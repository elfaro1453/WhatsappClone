package kuy.belajar.whatsappclone.recyclerview

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.chat_item_left.view.*
import kuy.belajar.whatsappclone.MessageChatActivity
import kuy.belajar.whatsappclone.model.Chat


/**
 * Created by Imam Fahrur Rofi on 20/10/20.
 */
class ChatItemLeftVH(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(chat: Chat, imageUrl: String) {
        itemView.run {
            if (chat.message.isNotBlank()) {
                sent_image.visibility = View.GONE

                text_message.text = chat.message
                text_message.visibility = View.VISIBLE
            } else {
                if (chat.url.isNotBlank()) {
                    text_message.visibility = View.GONE

                    Picasso.get().load(chat.url).fit().centerCrop().into(sent_image)
                    sent_image.visibility = View.VISIBLE
                }
            }
            text_seen.visibility =
                if (chat.isSeen != MessageChatActivity.IS_SEEN_FALSE) View.VISIBLE else View.GONE
            if (imageUrl.isNotBlank()) Picasso.get().load(imageUrl).fit().centerCrop()
                .into(profile_image)
        }
    }
}