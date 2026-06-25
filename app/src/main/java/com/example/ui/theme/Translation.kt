package com.example.ui.theme

import androidx.compose.ui.unit.LayoutDirection

enum class AppLanguage {
    EN, FA
}

object Translation {
    private val dictionary = mapOf(
        "app_name" to Pair("RoozAra", "روزآرا"),
        "timeline" to Pair("Timeline", "گاه‌شمار"),
        "analytics" to Pair("Analytics", "تحلیل زندگی"),
        "calendar" to Pair("Calendar", "تقویم"),
        "settings" to Pair("Settings", "تنظیمات"),
        "add_task" to Pair("Add Task", "افزودن کار"),
        "edit_task" to Pair("Edit Task", "ویرایش کار"),
        "title_label" to Pair("Task Title / Command", "عنوان کار یا دستور صوتی/متنی"),
        "title_placeholder" to Pair("e.g. Gym at 5 PM", "مثال: باشگاه ساعت ۵ عصر"),
        "time_label" to Pair("Time", "زمان"),
        "date_label" to Pair("Date", "تاریخ"),
        "energy_label" to Pair("Energy Level", "میزان انرژی"),
        "energy_low" to Pair("Low", "کم"),
        "energy_medium" to Pair("Medium", "متوسط"),
        "energy_high" to Pair("High", "زیاد"),
        "repeat_label" to Pair("Repeat", "تکرار"),
        "repeat_none" to Pair("None", "بدون تکرار"),
        "repeat_daily" to Pair("Daily", "روزانه"),
        "repeat_weekly" to Pair("Weekly", "هفتگی"),
        "reminder_label" to Pair("Reminder", "یادآور"),
        "reminder_30m" to Pair("30 minutes before", "۳۰ دقیقه قبل"),
        "reminder_none" to Pair("No reminder", "بدون یادآور"),
        "notification_style_label" to Pair("Notification Style", "لحن اعلان‌ها"),
        "notif_friendly" to Pair("Friendly", "دوستانه"),
        "notif_motivational" to Pair("Motivational", "انگیزشی"),
        "notif_serious" to Pair("Serious", "جدی"),
        "save" to Pair("Save", "ذخیره"),
        "cancel" to Pair("Cancel", "لغو"),
        "delete" to Pair("Delete", "حذف"),
        "empty_timeline" to Pair("Your timeline is empty. Transform chaos into calm.", "گاه‌شمار شما خالی است. هرج‌ومرج را به آرامش تبدیل کنید."),
        "quick_add_btn" to Pair("Quick Add", "افزودن سریع"),
        "smart_parse_btn" to Pair("AI Smart Parse", "پردازش هوشمند با هوش مصنوعی"),
        "parsing_loading" to Pair("AI is structuring your life...", "هوش مصنوعی در حال مرتب‌سازی است..."),
        "focus_mode_title" to Pair("Focus Mode", "حالت تمرکز"),
        "focus_mode_desc" to Pair("Isolate your mind. Only this task exists.", "ذهن خود را رها کنید. فقط این کار اهمیت دارد."),
        "exit_focus" to Pair("Exit Focus", "خروج از تمرکز"),
        "complete_task" to Pair("Complete", "انجام شد"),
        "reshuffled_tag" to Pair("Reshuffled", "بازتنظیم شده"),
        "reshuffle_btn" to Pair("Reshuffle Unfinished Tasks", "جابجایی کارهای انجام‌نشده"),
        "reshuffle_success" to Pair("Tasks moved to best available slots!", "کارها به بهترین زمان‌های خالی منتقل شدند!"),
        "drive_sync_title" to Pair("Google Drive Sync", "همگام‌سازی گوگل درایو"),
        "drive_sync_desc" to Pair("Backup and restore your timeline securely.", "پشتیبان‌گیری و بازیابی امن کارهای شما."),
        "backup_now" to Pair("Backup Now (JSON)", "پشتیبان‌گیری سریع (JSON)"),
        "restore_now" to Pair("Restore Now (JSON)", "بازیابی داده‌ها (JSON)"),
        "sync_status_idle" to Pair("Last synced: Local Offline First", "آخرین همگام‌سازی: محلی آفلاین"),
        "sync_status_success" to Pair("Backup created successfully!", "پشتیبان‌گیری با موفقیت انجام شد!"),
        "sync_status_restored" to Pair("Restore complete!", "بازیابی اطلاعات انجام شد!"),
        "productivity_score" to Pair("Productivity Score", "امتیاز بهره‌وری"),
        "score_label" to Pair("Score", "امتیاز"),
        "completed" to Pair("Completed", "انجام شده"),
        "missed" to Pair("Missed", "انجام نشده"),
        "pending" to Pair("Pending", "در انتظار"),
        "weekly_completion" to Pair("Weekly Completion Rate", "نرخ تکمیل هفتگی"),
        "most_productive" to Pair("Most Productive Day", "بهره‌ورترین روز"),
        "least_productive" to Pair("Least Productive Day", "کم‌بهره‌ورترین روز"),
        "consistency" to Pair("Consistency Trend", "روند پایداری"),
        "monthly_title" to Pair("Monthly Performance", "عملکرد ماهانه"),
        "monthly_disciplined" to Pair("Disciplined", "منظم و با‌اراده"),
        "monthly_focused" to Pair("Focused", "متمرکز و پرانرژی"),
        "monthly_irregular" to Pair("Irregular", "پراکنده"),
        "behavior_pattern" to Pair("Behavioral Patterns", "الگوهای رفتاری"),
        "pattern_desc_high" to Pair("You complete High Energy tasks best in mornings.", "کارهای پرانرژی را صبح‌ها بهتر انجام می‌دهید."),
        "pattern_desc_med" to Pair("Your consistency is stable through weekdays.", "میزان پایداری شما در طول روزهای هفته پایدار است."),
        "life_highlights" to Pair("Life Highlights", "افتخارات زندگی"),
        "longest_streak" to Pair("Longest Streak", "طولانی‌ترین زنجیره"),
        "days" to Pair("days", "روز"),
        "highest_focus" to Pair("Highest Focus Score", "بیشترین امتیاز تمرکز"),
        "clean_minimal_ui" to Pair("RoozAra converts simple inputs into a peaceful timeline of life.", "روزآرا ورودی‌های ساده شما را به یک گاه‌شمار آرام از زندگی تبدیل می‌کند."),
        "language_label" to Pair("Language / زبان", "زبان / Language"),
        "lang_en" to Pair("English", "انگلیسی"),
        "lang_fa" to Pair("Persian (فارسی)", "فارسی (Persian)"),
        "auto_reshuffle_toggle" to Pair("Auto Reshuffle Unfinished Tasks", "جابجایی خودکار کارهای انجام‌نشده"),
        "auto_reshuffle_desc" to Pair("Automatically moves incomplete tasks to open slots.", "انتقال خودکار کارهای معوقه به زمان‌های خالی روز."),
        "suggestion_title" to Pair("Energy scheduling suggestion", "پیشنهاد زمان‌بندی بر اساس انرژی"),
        "suggestion_text_high" to Pair("High energy detected. Scheduled for morning peak.", "انرژی زیاد تشخیص داده شد. برای اوج فعالیت صبحگاهی تنظیم شد."),
        "suggestion_text_low" to Pair("Low energy detected. Scheduled for evening winding down.", "انرژی کم تشخیص داده شد. برای استراحت عصرگاهی تنظیم شد."),
        "weekly_label" to Pair("Weekly Dashboard", "داشبورد هفتگی"),
        "monthly_label" to Pair("Monthly Dashboard", "داشبورد ماهانه"),
        "daily_label" to Pair("Daily Dashboard", "داشبورد روزانه"),
        "motivational_friendly_msg" to Pair("You are doing great! One step at a time.", "عالی هستی! قدم به قدم، به جلو حرکت کن."),
        "motivational_serious_msg" to Pair("Stay disciplined. Progress requires execution.", "منظم بمان. پیشرفت نیازمند اجراست."),
        "motivational_inspire_msg" to Pair("A calm mind creates an orderly life.", "ذهن آرام، زندگی منظمی خلق می‌کند.")
    )

    fun getString(key: String, language: AppLanguage): String {
        val pair = dictionary[key] ?: return key
        return if (language == AppLanguage.EN) pair.first else pair.second
    }

    fun getLayoutDirection(language: AppLanguage): LayoutDirection {
        return if (language == AppLanguage.FA) LayoutDirection.Rtl else LayoutDirection.Ltr
    }
}
