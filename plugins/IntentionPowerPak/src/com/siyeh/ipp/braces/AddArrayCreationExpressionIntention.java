/*
 * Copyright 2011 Bas Leijdekkers
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
package com.siyeh.ipp.braces;

import com.intellij.psi.PsiArrayInitializerExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.util.IncorrectOperationException;
import com.siyeh.IntentionPowerPackBundle;
import com.siyeh.ipp.base.MutablyNamedIntention;
import com.siyeh.ipp.base.PsiElementPredicate;
import org.jetbrains.annotations.NotNull;

public class AddArrayCreationExpressionIntention extends MutablyNamedIntention {

  @Override
  @NotNull
  protected PsiElementPredicate getElementPredicate() {
    return new ArrayCreationExpressionPredicate();
  }

  @Override
  protected String getTextForElement(PsiElement element) {
    final PsiArrayInitializerExpression arrayInitializerExpression =
      (PsiArrayInitializerExpression)element;
    final PsiType type = arrayInitializerExpression.getType();
    assert type != null;
    return IntentionPowerPackBundle.message(
      "add.array.creation.expression.intention.name",
      type.getPresentableText());
  }

  @Override
  protected void processIntention(@NotNull PsiElement element)
    throws IncorrectOperationException {
    final PsiArrayInitializerExpression arrayInitializerExpression =
      (PsiArrayInitializerExpression)element;
    final PsiType type = arrayInitializerExpression.getType();
    if (type == null) {
      return;
    }
    final String typeText = type.getCanonicalText();
    final String newExpressionText =
      "new " + typeText + arrayInitializerExpression.getText();
    replaceExpression(newExpressionText, arrayInitializerExpression);
  }
}
