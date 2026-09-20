/**
 * ==============================================================================
 * GOOGLE APPS SCRIPT: ALL-IN-ONE BACKEND FOR ANDROID APP
 * Supports: LIKES, DISLIKES, COMMENTS, & REMOTE SETTINGS
 * ==============================================================================
 * 
 * HOW TO INSTALL IN GOOGLE SHEETS:
 * 1. Open Google Sheets (https://sheets.google.com) and create a New Spreadsheet.
 * 2. Click "Extensions" -> "Apps Script".
 * 3. Delete any default code in Code.gs and PASTE THIS ENTIRE SCRIPT.
 * 4. Click the "Save" icon (Ctrl + S / Cmd + S).
 * 5. (Optional) Run the function 'initializeSheets()' once to automatically 
 *    generate and format all 3 tabs ("Reactions", "Comments", "Settings").
 * 6. Click "Deploy" (top right) -> "New deployment".
 * 7. Click gear icon -> Select "Web app".
 *    - Description: "App Backend v1.0"
 *    - Execute as: "Me" (your email)
 *    - Who has access: "Anyone" (CRITICAL: Must be "Anyone" so the app can connect!)
 * 8. Click "Deploy", authorize permissions, and COPY the Web App URL (ends in /exec).
 * 9. Paste this URL into your app's Config.java (GOOGLE_SHEET_WEBAPP_URL)
 *    or config.json ("google_sheet_url").
 * ==============================================================================
 */

// Tab Names
const SHEET_COMMENTS = "Comments";
const SHEET_REACTIONS = "Reactions";
const SHEET_SETTINGS = "Settings";
const SHEET_USER_VOTES = "UserVotes";

/**
 * Automatically creates and formats all tabs if they do not exist
 */
function initializeSheets() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();

  // 1. Comments Sheet
  let commentsSheet = ss.getSheetByName(SHEET_COMMENTS);
  if (!commentsSheet) {
    commentsSheet = ss.insertSheet(SHEET_COMMENTS);
    const headers = [["Date & Time", "Article ID", "Article Title", "Name", "Email", "Comment", "Status"]];
    commentsSheet.getRange(1, 1, 1, headers[0].length).setValues(headers)
      .setBackground("#1E293B").setFontColor("#FFFFFF").setFontWeight("bold");
    commentsSheet.setFrozenRows(1);
    commentsSheet.setColumnWidth(1, 160);
    commentsSheet.setColumnWidth(2, 120);
    commentsSheet.setColumnWidth(3, 220);
    commentsSheet.setColumnWidth(4, 140);
    commentsSheet.setColumnWidth(5, 180);
    commentsSheet.setColumnWidth(6, 300);
    commentsSheet.setColumnWidth(7, 100);
  }

  // 2. Reactions Sheet (Likes / Dislikes)
  let reactionsSheet = ss.getSheetByName(SHEET_REACTIONS);
  if (!reactionsSheet) {
    reactionsSheet = ss.insertSheet(SHEET_REACTIONS);
    const headers = [["Article ID", "Article Title", "Likes", "Dislikes", "Total Reactions", "Last Updated"]];
    reactionsSheet.getRange(1, 1, 1, headers[0].length).setValues(headers)
      .setBackground("#0F766E").setFontColor("#FFFFFF").setFontWeight("bold");
    reactionsSheet.setFrozenRows(1);
    reactionsSheet.setColumnWidth(1, 130);
    reactionsSheet.setColumnWidth(2, 250);
    reactionsSheet.setColumnWidth(3, 90);
    reactionsSheet.setColumnWidth(4, 90);
    reactionsSheet.setColumnWidth(5, 120);
    reactionsSheet.setColumnWidth(6, 170);
  }

  // 3. User Votes Sheet (Prevents duplicate likes/dislikes from the same device)
  let votesSheet = ss.getSheetByName(SHEET_USER_VOTES);
  if (!votesSheet) {
    votesSheet = ss.insertSheet(SHEET_USER_VOTES);
    const headers = [["Article ID", "Device ID", "Vote", "Timestamp"]];
    votesSheet.getRange(1, 1, 1, headers[0].length).setValues(headers)
      .setBackground("#0D9488").setFontColor("#FFFFFF").setFontWeight("bold");
    votesSheet.setFrozenRows(1);
    votesSheet.setColumnWidth(1, 150);
    votesSheet.setColumnWidth(2, 220);
    votesSheet.setColumnWidth(3, 100);
    votesSheet.setColumnWidth(4, 180);
  }

  // 3. Settings Sheet
  let settingsSheet = ss.getSheetByName(SHEET_SETTINGS);
  if (!settingsSheet) {
    settingsSheet = ss.insertSheet(SHEET_SETTINGS);
    const headers = [["Key", "Value", "Description"]];
    settingsSheet.getRange(1, 1, 1, headers[0].length).setValues(headers)
      .setBackground("#4338CA").setFontColor("#FFFFFF").setFontWeight("bold");
    settingsSheet.setFrozenRows(1);
    settingsSheet.setColumnWidth(1, 180);
    settingsSheet.setColumnWidth(2, 250);
    settingsSheet.setColumnWidth(3, 300);

    // Populate Default Settings
    const defaultSettings = [
      ["app_name", "Malayalam Movie List", "Public application name"],
      ["app_version", "40.0", "Current active version in Play Store"],
      ["update_required", "false", "Set 'true' to force users to update"],
      ["update_message", "A new version of the app is available on Google Play!", "Alert prompt message"],
      ["update_url", "https://play.google.com/store/apps/details?id=com.shobmc.san", "Play Store link"],
      ["banner_ads_enabled", "true", "Remote toggle for banner ads (true/false)"],
      ["interstitial_ads_enabled", "true", "Remote toggle for interstitial ads (true/false)"],
      ["maintenance_mode", "false", "Set 'true' if backend is under maintenance"],
      ["announcement_ticker", "Welcome to Malayalam Movie List & News!", "Marquee announcement banner text"]
    ];
    settingsSheet.getRange(2, 1, defaultSettings.length, 3).setValues(defaultSettings);
  }

  // Remove default "Sheet1" if empty
  const defaultSheet = ss.getSheetByName("Sheet1");
  if (defaultSheet && ss.getSheets().length > 1) {
    try { ss.deleteSheet(defaultSheet); } catch (e) {}
  }
}

/**
 * Handle POST Requests (Add Comment, Like, Dislike, or Update Setting)
 */
function doPost(e) {
  try {
    initializeSheets();
    const params = e.parameter || {};
    const action = (params.action || "").toLowerCase().trim();

    // 1. LIKE, DISLIKE, or REACTION (With 1-vote-per-device enforcement)
    if (action === "reaction" || action === "vote" || action === "like" || action === "dislike") {
      const articleId = params.articleId || params.newsId || params.id || "general";
      const title = params.title || "Article";
      const deviceId = params.deviceId || ("device_" + (params.timestamp || Date.now()));
      let vote = (params.vote || action || "").toLowerCase().trim();
      if (vote !== "like" && vote !== "dislike" && vote !== "none") {
        vote = "none";
      }

      const result = updateReactionWithDevice(articleId, title, deviceId, vote);
      return createJsonResponse({
        status: "success",
        articleId: articleId,
        userVote: result.userVote,
        likes: result.likes,
        dislikes: result.dislikes
      });
    }

    // 2. Add New Comment
    if (action === "addcomment" || params.comment) {
      const articleId = params.articleId || params.newsId || params.id || "general";
      const title = params.title || "";
      const name = params.name || "Anonymous Reader";
      const email = params.email || "";
      const comment = params.comment || "";
      const date = new Date().toLocaleString();

      const ss = SpreadsheetApp.getActiveSpreadsheet();
      const commentsSheet = ss.getSheetByName(SHEET_COMMENTS);
      commentsSheet.appendRow([date, articleId, title, name, email, comment, "Approved"]);

      return createJsonResponse({ status: "success", message: "Comment posted successfully" });
    }

    // 3. Update Remote Setting (Admin action)
    if (action === "updatesetting") {
      const key = params.key;
      const value = params.value;
      if (!key) {
        return createJsonResponse({ status: "error", message: "Missing setting key" });
      }
      setSettingValue(key, value);
      return createJsonResponse({ status: "success", message: "Setting updated", key: key, value: value });
    }

    // Fallback: Default comment append for backward compatibility
    return createJsonResponse({ status: "success", message: "Request received" });

  } catch (error) {
    return createJsonResponse({ status: "error", message: error.toString() });
  }
}

/**
 * Handle GET Requests (Fetch Comments, Reactions, or Settings)
 */
function doGet(e) {
  try {
    initializeSheets();
    const params = e.parameter || {};
    const action = (params.action || "").toLowerCase().trim();
    const articleId = params.articleId || params.newsId || params.id || "";

    // 1. Get All Settings (Default if no action provided)
    if (!action || action === "getsettings" || action === "settings") {
      const settings = getAllSettings();
      return createJsonResponse({ status: "success", settings: settings });
    }

    // 2. Get Combined Article Data (Reactions + Comments + User Vote)
    if (action === "getarticledata") {
      const deviceId = params.deviceId || "";
      const reactions = getArticleReactions(articleId);
      const userVote = getUserVote(articleId, deviceId);
      const comments = getArticleComments(articleId);
      return createJsonResponse({
        status: "success",
        articleId: articleId,
        likes: reactions.likes,
        dislikes: reactions.dislikes,
        userVote: userVote,
        comments: comments
      });
    }

    // 3. Get Article Reactions Only
    if (action === "getreactions") {
      const deviceId = params.deviceId || "";
      const reactions = getArticleReactions(articleId);
      const userVote = getUserVote(articleId, deviceId);
      return createJsonResponse({
        status: "success",
        articleId: articleId,
        likes: reactions.likes,
        dislikes: reactions.dislikes,
        userVote: userVote
      });
    }

    // 4. Get Article Comments Only
    if (action === "getcomments") {
      const comments = getArticleComments(articleId);
      return createJsonResponse({
        status: "success",
        articleId: articleId,
        count: comments.length,
        comments: comments
      });
    }

    return createJsonResponse({ status: "error", message: "Invalid action: " + action });

  } catch (error) {
    return createJsonResponse({ status: "error", message: error.toString() });
  }
}

// ==============================================================================
// HELPER FUNCTIONS
// ==============================================================================

/**
 * Update reaction with 1-vote-per-device enforcement
 */
function updateReactionWithDevice(articleId, title, deviceId, newVote) {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  let votesSheet = ss.getSheetByName(SHEET_USER_VOTES);
  if (!votesSheet) {
    initializeSheets();
    votesSheet = ss.getSheetByName(SHEET_USER_VOTES);
  }
  const reactionsSheet = ss.getSheetByName(SHEET_REACTIONS);

  // 1. Check Previous Vote of this Device for this Article
  let previousVote = "none";
  let userVoteRow = -1;
  const votesData = votesSheet.getDataRange().getValues();

  for (let i = 1; i < votesData.length; i++) {
    if (String(votesData[i][0]).trim() === String(articleId).trim() &&
        String(votesData[i][1]).trim() === String(deviceId).trim()) {
      userVoteRow = i + 1;
      previousVote = String(votesData[i][2]).toLowerCase().trim();
      break;
    }
  }

  // 2. Locate or create Article in Reactions Sheet
  const reactionsData = reactionsSheet.getDataRange().getValues();
  let reactionRow = -1;
  let currentLikes = 0;
  let currentDislikes = 0;

  for (let i = 1; i < reactionsData.length; i++) {
    if (String(reactionsData[i][0]).trim() === String(articleId).trim()) {
      reactionRow = i + 1;
      currentLikes = Number(reactionsData[i][2]) || 0;
      currentDislikes = Number(reactionsData[i][3]) || 0;
      break;
    }
  }

  // If vote hasn't changed, return current count immediately
  if (previousVote === newVote) {
    return { likes: currentLikes, dislikes: currentDislikes, userVote: newVote };
  }

  // 3. Adjust Likes and Dislikes delta
  // Cancel previous vote effect
  if (previousVote === "like") {
    currentLikes = Math.max(0, currentLikes - 1);
  } else if (previousVote === "dislike") {
    currentDislikes = Math.max(0, currentDislikes - 1);
  }

  // Apply new vote effect
  if (newVote === "like") {
    currentLikes += 1;
  } else if (newVote === "dislike") {
    currentDislikes += 1;
  }

  const now = new Date().toLocaleString();

  // 4. Update Reactions Sheet
  if (reactionRow > 0) {
    reactionsSheet.getRange(reactionRow, 3).setValue(currentLikes);
    reactionsSheet.getRange(reactionRow, 4).setValue(currentDislikes);
    reactionsSheet.getRange(reactionRow, 5).setValue(currentLikes + currentDislikes);
    reactionsSheet.getRange(reactionRow, 6).setValue(now);
  } else {
    reactionsSheet.appendRow([articleId, title, currentLikes, currentDislikes, currentLikes + currentDislikes, now]);
  }

  // 5. Update or Insert User Votes Record
  if (userVoteRow > 0) {
    votesSheet.getRange(userVoteRow, 3).setValue(newVote);
    votesSheet.getRange(userVoteRow, 4).setValue(now);
  } else if (newVote !== "none") {
    votesSheet.appendRow([articleId, deviceId, newVote, now]);
  }

  return { likes: currentLikes, dislikes: currentDislikes, userVote: newVote };
}

/**
 * Retrieve this specific device's vote for an article ("like", "dislike", or "none")
 */
function getUserVote(articleId, deviceId) {
  if (!articleId || !deviceId) return "none";
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const votesSheet = ss.getSheetByName(SHEET_USER_VOTES);
  if (!votesSheet) return "none";

  const votesData = votesSheet.getDataRange().getValues();
  for (let i = 1; i < votesData.length; i++) {
    if (String(votesData[i][0]).trim() === String(articleId).trim() &&
        String(votesData[i][1]).trim() === String(deviceId).trim()) {
      return String(votesData[i][2]).toLowerCase().trim() || "none";
    }
  }
  return "none";
}

/**
 * Legacy updateReaction wrapper
 */
function updateReaction(articleId, title, type) {
  return updateReactionWithDevice(articleId, title, "legacy_user", type);
}

/**
 * Retrieve likes and dislikes for an article
 */
function getArticleReactions(articleId) {
  if (!articleId) return { likes: 0, dislikes: 0 };
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const sheet = ss.getSheetByName(SHEET_REACTIONS);
  const data = sheet.getDataRange().getValues();

  for (let i = 1; i < data.length; i++) {
    if (String(data[i][0]).trim() === String(articleId).trim()) {
      return {
        likes: Number(data[i][2]) || 0,
        dislikes: Number(data[i][3]) || 0
      };
    }
  }
  return { likes: 0, dislikes: 0 };
}

/**
 * Retrieve all approved comments for an article
 */
function getArticleComments(articleId) {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const sheet = ss.getSheetByName(SHEET_COMMENTS);
  const data = sheet.getDataRange().getValues();
  const comments = [];

  for (let i = data.length - 1; i >= 1; i--) {
    const row = data[i];
    const rowArticleId = String(row[1]).trim();
    const status = String(row[6] || "").toLowerCase().trim();

    // If articleId is specified, match it. If articleId is empty, return latest.
    if (!articleId || rowArticleId === String(articleId).trim()) {
      if (status !== "rejected" && status !== "hidden") {
        comments.push({
          date: row[0] ? String(row[0]) : "Recently",
          name: row[3] ? String(row[3]) : "Reader",
          comment: row[5] ? String(row[5]) : ""
        });
      }
    }
  }
  return comments;
}

/**
 * Retrieve all key-value settings from the Settings sheet
 */
function getAllSettings() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const sheet = ss.getSheetByName(SHEET_SETTINGS);
  const data = sheet.getDataRange().getValues();
  const settings = {};

  for (let i = 1; i < data.length; i++) {
    const key = String(data[i][0] || "").trim();
    const value = String(data[i][1] != null ? data[i][1] : "");
    if (key) {
      settings[key] = value;
    }
  }
  return settings;
}

/**
 * Set a key-value setting in the Settings sheet
 */
function setSettingValue(key, value) {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const sheet = ss.getSheetByName(SHEET_SETTINGS);
  const data = sheet.getDataRange().getValues();

  for (let i = 1; i < data.length; i++) {
    if (String(data[i][0] || "").trim() === key.trim()) {
      sheet.getRange(i + 1, 2).setValue(value);
      return;
    }
  }
  // Not found, append
  sheet.appendRow([key, value, "Custom setting added via API"]);
}

/**
 * Output helper to return formatted JSON
 */
function createJsonResponse(data) {
  return ContentService.createTextOutput(JSON.stringify(data))
    .setMimeType(ContentService.MimeType.JSON);
}
