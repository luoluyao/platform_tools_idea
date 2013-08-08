/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.tasks.trello.model;

import com.google.gson.annotations.SerializedName;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.intellij.tasks.trello.model.TrelloLabel.*;

/**
 * @author Mikhail Golubev
 */
public class TrelloCard extends TrelloModel {

  private String idBoard, idList;
  private int idShort;
  private List<String> idMembers, idMembersVoted, idChecklists;
  private String name;
  @SerializedName("desc")
  private String description;
  private String url, shortUrl;
  @SerializedName("due")
  private Date dueDate;
  private boolean closed;
  private double pos;
  private List<TrelloLabel> labels;
  @SerializedName("actions")
  private List<TrelloCommentAction> comments = ContainerUtil.emptyList();
  /**
   * This field is not part of card representation downloaded from server
   * and set explicitly in {@code com.intellij.tasks.trello.TrelloRepository}
   */
  private boolean isVisible = true;

  /**
   * Serialization constructor
   */
  @SuppressWarnings("UnusedDeclaration")
  public TrelloCard() {
  }

  @Override
  public String toString() {
    return String.format("TrelloCard(id='%s', name='%s')", getId(), name);
  }

  @NotNull
  public String getIdBoard() {
    return idBoard;
  }

  @NotNull
  public String getIdList() {
    return idList;
  }

  @NotNull
  public List<String> getIdMembers() {
    return idMembers;
  }

  @NotNull
  @Attribute("name")
  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  public int getIdShort() {
    return idShort;
  }

  @NotNull
  public List<String> getIdMembersVoted() {
    return idMembersVoted;
  }

  @NotNull
  public List<String> getIdChecklists() {
    return idChecklists;
  }

  @NotNull
  public String getDescription() {
    return description;
  }

  @Nullable
  public Date getDueDate() {
    return dueDate;
  }

  @NotNull
  public String getUrl() {
    return url;
  }

  @Nullable
  public String getShortUrl() {
    return shortUrl;
  }

  public boolean isClosed() {
    return closed;
  }

  public double getPos() {
    return pos;
  }

  @NotNull
  public List<TrelloLabel> getLabels() {
    return labels;
  }

  @NotNull
  public List<TrelloCommentAction> getComments() {
    return comments;
  }

  @NotNull
  public Set<LabelColor> getColors() {
    if (labels == null || labels.isEmpty()) {
      return EnumSet.noneOf(LabelColor.class);
    }
    return EnumSet.copyOf(ContainerUtil.map(labels, new Function<TrelloLabel, LabelColor>() {
      @Override
      public LabelColor fun(TrelloLabel label) {
        return label.getColor();
      }
    }));
  }

  public boolean isVisible() {
    return isVisible;
  }

  public void setVisible(boolean visible) {
    isVisible = visible;
  }
}