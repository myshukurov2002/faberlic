package com.company;

import lombok.SneakyThrows;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.*;

public class MyBot extends TelegramLongPollingBot {

    // Faberlic mahsulotlar ma'lumotlari
    private Map<String, Product> products = new HashMap<>();

    // Savatcha ma'lumotlari (userId -> mahsulotlar)
    private Map<Long, List<CartItem>> userCarts = new HashMap<>();

    private Map<Long, String> userStates = new HashMap<>();

    public MyBot() {
        super("8158187629:AAHXSz0h8aUBfOaQkbSsAyu0EkH3rz7EQAg");
        initializeProducts();
    }

    @Override
    public String getBotUsername() {
        return "https://t.me/faberlic_sirdaryobot";
    }

    // Mahsulotlarni boshlang'ich ma'lumotlar bilan to'ldirish
    private void initializeProducts() {
        products.put("parfum1", new Product("parfum1", "Faberlic Zen 🌸",
                "Ayollar uchun nozik va romantik hid", 150000,
                "https://images.unsplash.com/photo-1594035910387-fea47794261f?w=400"));

        products.put("parfum2", new Product("parfum2", "Faberlic Royal 👑",
                "Erkaklar uchun kuchli va charismatik parfyum", 180000,
                "https://images.unsplash.com/photo-1588405748880-12d1d2a59d32?w=400"));

        products.put("cream1", new Product("cream1", "Faberlic Anti-Age Krem 💆‍♀️",
                "Qarishga qarshi samarali krem", 120000,
                "https://images.unsplash.com/photo-1556228453-efd6c1ff04f6?w=400"));

        products.put("lipstick1", new Product("lipstick1", "Faberlic Matte Lipstick 💄",
                "Uzoq davom etuvchi mat lab bo'yog'i", 65000,
                "https://images.unsplash.com/photo-1586495777744-4413f21062fa?w=400"));

        products.put("shampoo1", new Product("shampoo1", "Faberlic Repair Shampoo 🧴",
                "Zarar ko'rgan sochlarni tiklash uchun", 85000,
                "https://images.unsplash.com/photo-1571781926291-c477ebfd024b?w=400"));
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            handleMessage(update.getMessage());
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        }
    }

    @SneakyThrows
    private void handleMessage(Message message) {
        Long chatId = message.getChatId();
        String text = message.getText();

        if ("/start".equals(text)) {
            sendWelcomeMessage(chatId);
        } else if ("🛍️ Mahsulotlar".equals(text)) {
            showProductCategories(chatId);
        } else if ("🛒 Savatcha".equals(text)) {
            showCart(chatId);
        } else if ("ℹ️ Ma'lumot".equals(text)) {
            sendInfo(chatId);
        } else if ("📞 Aloqa".equals(text)) {
            sendContact(chatId);
        } else {
            // Noma'lum xabar
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText("Iltimos, menyudan tanlang 👇");
            execute(sendMessage);
        }
    }

    @SneakyThrows
    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();
        String data = callbackQuery.getData();

        if (data.startsWith("category_")) {
            String category = data.substring(9);
            showProductsByCategory(chatId, category);
        } else if (data.startsWith("product_")) {
            String productId = data.substring(8);
            showProductDetails(chatId, productId);
        } else if (data.startsWith("add_to_cart_")) {
            String productId = data.substring(12);
            addToCart(chatId, productId);
        } else if (data.equals("show_cart")) {
            showCart(chatId);
        } else if (data.startsWith("remove_from_cart_")) {
            String productId = data.substring(17);
            removeFromCart(chatId, productId);
        } else if (data.equals("clear_cart")) {
            clearCart(chatId);
        } else if (data.equals("checkout")) {
            processCheckout(chatId);
        }
    }

    @SneakyThrows
    private void sendWelcomeMessage(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(
                "🌟 *Faberlic Online Do'koniga Xush Kelibsiz!* 🌟\n\n" +
                        "💄 Go'zallik va parvarish mahsulotlari\n" +
                        "🌸 Parfyumeriya\n" +
                        "✨ Yuqori sifat, arzon narx\n\n" +
                        "Menyudan kerakli bo'limni tanlang:"
        );
        sendMessage.setParseMode("Markdown");
        sendMessage.setReplyMarkup(getMainKeyboard());
        execute(sendMessage);
    }

    @SneakyThrows
    private void showProductCategories(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("🛍️ *Mahsulot kategoriyalarini tanlang:*");
        sendMessage.setParseMode("Markdown");
        sendMessage.setReplyMarkup(getCategoryKeyboard());
        execute(sendMessage);
    }

    @SneakyThrows
    private void showProductsByCategory(Long chatId, String category) {
        List<Product> categoryProducts = getProductsByCategory(category);

        if (categoryProducts.isEmpty()) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText("❌ Bu kategoriyada hozircha mahsulotlar yo'q");
            execute(sendMessage);
            return;
        }

        for (Product product : categoryProducts) {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId);
            sendPhoto.setPhoto(new InputFile(product.getImageUrl()));
            sendPhoto.setCaption(
                    "*" + product.getName() + "*\n\n" +
                            product.getDescription() + "\n\n" +
                            "💰 Narxi: *" + formatPrice(product.getPrice()) + " so'm*"
            );
            sendPhoto.setParseMode("Markdown");
            sendPhoto.setReplyMarkup(getProductKeyboard(product.getId()));
            execute(sendPhoto);
        }
    }

    @SneakyThrows
    private void showProductDetails(Long chatId, String productId) {
        Product product = products.get(productId);
        if (product == null) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText("❌ Mahsulot topilmadi");
            execute(sendMessage);
            return;
        }

        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId);
        sendPhoto.setPhoto(new InputFile(product.getImageUrl()));
        sendPhoto.setCaption(
                "🌟 *" + product.getName() + "*\n\n" +
                        "📝 Tavsif: " + product.getDescription() + "\n\n" +
                        "💰 Narxi: *" + formatPrice(product.getPrice()) + " so'm*\n\n" +
                        "Savatchaga qo'shishni xohlaysizmi?"
        );
        sendPhoto.setParseMode("Markdown");
        sendPhoto.setReplyMarkup(getAddToCartKeyboard(productId));
        execute(sendPhoto);
    }

    @SneakyThrows
    private void addToCart(Long chatId, String productId) {
        Product product = products.get(productId);
        if (product == null) return;

        userCarts.computeIfAbsent(chatId, k -> new ArrayList<>());
        List<CartItem> cart = userCarts.get(chatId);

        // Agar mahsulot savatchada bo'lsa, miqdorini oshir
        boolean found = false;
        for (CartItem item : cart) {
            if (item.getProductId().equals(productId)) {
                item.setQuantity(item.getQuantity() + 1);
                found = true;
                break;
            }
        }

        if (!found) {
            cart.add(new CartItem(productId, 1));
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("✅ *" + product.getName() + "* savatchaga qo'shildi!");
        sendMessage.setParseMode("Markdown");
        sendMessage.setReplyMarkup(getAfterAddKeyboard());
        execute(sendMessage);
    }

    @SneakyThrows
    private void showCart(Long chatId) {
        List<CartItem> cart = userCarts.get(chatId);

        if (cart == null || cart.isEmpty()) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText("🛒 Savatchangiz bo'sh\n\nMahsulotlarni ko'rish uchun /start bosing");
            execute(sendMessage);
            return;
        }

        StringBuilder message = new StringBuilder("🛒 *Sizning savatchangiz:*\n\n");
        int totalPrice = 0;

        for (CartItem item : cart) {
            Product product = products.get(item.getProductId());
            if (product != null) {
                int itemTotal = product.getPrice() * item.getQuantity();
                totalPrice += itemTotal;

                message.append("• ").append(product.getName()).append("\n");
                message.append("  Miqdori: ").append(item.getQuantity()).append(" x ");
                message.append(formatPrice(product.getPrice())).append(" = ");
                message.append(formatPrice(itemTotal)).append(" so'm\n\n");
            }
        }

        message.append("💰 *Jami: ").append(formatPrice(totalPrice)).append(" so'm*");

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(message.toString());
        sendMessage.setParseMode("Markdown");
        sendMessage.setReplyMarkup(getCartKeyboard());
        execute(sendMessage);
    }

    @SneakyThrows
    private void removeFromCart(Long chatId, String productId) {
        List<CartItem> cart = userCarts.get(chatId);
        if (cart != null) {
            cart.removeIf(item -> item.getProductId().equals(productId));
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("🗑️ Mahsulot savatchadan olib tashlandi");
        execute(sendMessage);

        showCart(chatId);
    }

    @SneakyThrows
    private void clearCart(Long chatId) {
        userCarts.remove(chatId);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("🗑️ Savatcha tozalandi");
        execute(sendMessage);
    }

    @SneakyThrows
    private void processCheckout(Long chatId) {
        List<CartItem> cart = userCarts.get(chatId);
        if (cart == null || cart.isEmpty()) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText("❌ Savatcha bo'sh");
            execute(sendMessage);
            return;
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(
                "📞 *Buyurtmani tasdiqlash uchun:*\n\n" +
                        "☎️ Telefon: +998 90 123 45 67\n" +
                        "📱 Telegram: @faberlic_admin\n\n" +
                        "Bizning operatorlarimiz siz bilan bog'lanib, " +
                        "buyurtmani tasdiqlaydi va yetkazib berish vaqtini belgilaydi.\n\n" +
                        "🚚 Toshkent bo'yicha - BEPUL yetkazib berish!"
        );
        sendMessage.setParseMode("Markdown");
        execute(sendMessage);
    }

    @SneakyThrows
    private void sendInfo(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(
                "ℹ️ *Faberlic haqida:*\n\n" +
                        "🌟 30 yildan ortiq tajriba\n" +
                        "🌍 20+ mamlakatda faoliyat\n" +
                        "🔬 O'z laboratoriyalari\n" +
                        "✅ Sertifikatlangan mahsulotlar\n" +
                        "💯 Kafolat va sifat\n\n" +
                        "🎯 *Bizning afzalliklarimiz:*\n" +
                        "• Tez yetkazib berish\n" +
                        "• Arzon narxlar\n" +
                        "• Keng assortiment\n" +
                        "• Professional maslahat"
        );
        sendMessage.setParseMode("Markdown");
        execute(sendMessage);
    }

    @SneakyThrows
    private void sendContact(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(
                "📞 *Biz bilan bog'laning:*\n\n" +
                        "☎️ Telefon: +998 90 123 45 67\n" +
                        "📱 Telegram: @faberlic_admin\n" +
                        "📧 Email: info@faberlic.uz\n\n" +
                        "🕒 *Ish vaqti:*\n" +
                        "Dushanba - Shanba: 9:00 - 18:00\n" +
                        "Yakshanba: 10:00 - 16:00\n\n" +
                        "📍 *Manzil:*\n" +
                        "Toshkent sh., Chilonzor t., A.Temur ko'chasi 12-uy"
        );
        sendMessage.setParseMode("Markdown");
        execute(sendMessage);
    }

    // Klaviaturalar
    private ReplyKeyboardMarkup getMainKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("🛍️ Mahsulotlar"));
        row1.add(new KeyboardButton("🛒 Savatcha"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("ℹ️ Ma'lumot"));
        row2.add(new KeyboardButton("📞 Aloqa"));

        rows.add(row1);
        rows.add(row2);
        keyboard.setKeyboard(rows);

        return keyboard;
    }

    private InlineKeyboardMarkup getCategoryKeyboard() {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("🌸 Parfyumeriya")
                .callbackData("category_perfume")
                .build());

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(InlineKeyboardButton.builder()
                .text("💆‍♀️ Parvarish")
                .callbackData("category_skincare")
                .build());

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(InlineKeyboardButton.builder()
                .text("💄 Dekorativ kosmetika")
                .callbackData("category_makeup")
                .build());

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        keyboard.setKeyboard(rows);

        return keyboard;
    }

    private InlineKeyboardMarkup getProductKeyboard(String productId) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("📋 Batafsil")
                .callbackData("product_" + productId)
                .build());
        row1.add(InlineKeyboardButton.builder()
                .text("🛒 Savatchaga")
                .callbackData("add_to_cart_" + productId)
                .build());

        rows.add(row1);
        keyboard.setKeyboard(rows);

        return keyboard;
    }

    private InlineKeyboardMarkup getAddToCartKeyboard(String productId) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("🛒 Savatchaga qo'shish")
                .callbackData("add_to_cart_" + productId)
                .build());

        rows.add(row1);
        keyboard.setKeyboard(rows);

        return keyboard;
    }

    private InlineKeyboardMarkup getAfterAddKeyboard() {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("🛒 Savatchani ko'rish")
                .callbackData("show_cart")
                .build());

        rows.add(row1);
        keyboard.setKeyboard(rows);

        return keyboard;
    }

    private InlineKeyboardMarkup getCartKeyboard() {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("💳 Buyurtma berish")
                .callbackData("checkout")
                .build());

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(InlineKeyboardButton.builder()
                .text("🗑️ Tozalash")
                .callbackData("clear_cart")
                .build());

        rows.add(row1);
        rows.add(row2);
        keyboard.setKeyboard(rows);

        return keyboard;
    }

    // Yordamchi metodlar
    private List<Product> getProductsByCategory(String category) {
        List<Product> result = new ArrayList<>();

        switch (category) {
            case "perfume":
                result.add(products.get("parfum1"));
                result.add(products.get("parfum2"));
                break;
            case "skincare":
                result.add(products.get("cream1"));
                result.add(products.get("shampoo1"));
                break;
            case "makeup":
                result.add(products.get("lipstick1"));
                break;
        }

        return result;
    }

    private String formatPrice(int price) {
        return String.format("%,d", price).replace(',', ' ');
    }

    // Mahsulot klassi
    static class Product {
        private String id;
        private String name;
        private String description;
        private int price;
        private String imageUrl;

        public Product(String id, String name, String description, int price, String imageUrl) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.price = price;
            this.imageUrl = imageUrl;
        }

        // Getters
        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public int getPrice() { return price; }
        public String getImageUrl() { return imageUrl; }
    }

    // Savatcha elementi klassi
    static class CartItem {
        private String productId;
        private int quantity;

        public CartItem(String productId, int quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        // Getters and Setters
        public String getProductId() { return productId; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
}