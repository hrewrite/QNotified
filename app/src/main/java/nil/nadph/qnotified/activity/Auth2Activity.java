/* QNotified - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2020 cinit@github.com
 * https://github.com/cinit/QNotified
 *
 * This software is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.  If not, see
 * <https://www.gnu.org/licenses/>.
 */
package nil.nadph.qnotified.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;
import com.tencent.mobileqq.widget.BounceScrollView;
import nil.nadph.qnotified.chiral.ChiralCarbonHelper;
import nil.nadph.qnotified.chiral.Molecule;
import nil.nadph.qnotified.chiral.MoleculeView;
import nil.nadph.qnotified.chiral.PubChemStealer;
import nil.nadph.qnotified.ui.CustomDialog;
import nil.nadph.qnotified.ui.ResUtils;
import nil.nadph.qnotified.ui.ViewBuilder;
import nil.nadph.qnotified.util.LicenseStatus;
import nil.nadph.qnotified.util.Utils;

import java.util.HashSet;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static nil.nadph.qnotified.ui.ViewBuilder.newLinearLayoutParams;
import static nil.nadph.qnotified.util.Utils.*;

@SuppressLint("Registered")
public class Auth2Activity extends IphoneTitleBarActivityCompat implements View.OnClickListener, DialogInterface.OnClickListener, Runnable {

    private MoleculeView moleculeView;
    private TextView tvSelectedCount, newOne, reset;
    private Button nextStep;
    private Molecule currMol;
    private AlertDialog makingMol = null;
    private int refreshId = 0;
    private HashSet<Integer> mChiralCarbons;

    @Override
    public boolean doOnCreate(Bundle bundle) {
        super.doOnCreate(bundle);
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ViewGroup.LayoutParams mmlp = new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        ViewGroup bounceScrollView = new BounceScrollView(this, null);
        bounceScrollView.setLayoutParams(mmlp);
        bounceScrollView.addView(ll, new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        LinearLayout.LayoutParams fixlp = new LinearLayout.LayoutParams(MATCH_PARENT, dip2px(this, 48));
        LinearLayout.LayoutParams lp_mw = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);

        TextView tv = new TextView(this);
        tv.setTextSize(20);
        tv.setTextColor(ResUtils.skin_black);
        tv.setText("QNotified 高级验证\n为防止本软件被不合理使用, 您需要完成以下验证以激活本软件部分功能.");
        ll.addView(tv, lp_mw);
        TextView tv2 = new TextView(this);
        tv2.setTextSize(18);
        tv2.setTextColor(ResUtils.skin_black);
        tv2.setText("请点击(或长按)以选出下方有机物中的所有手性碳原子, 然后点击下一步. 如果您觉得下方分子过于复杂, 您可以尝试点击 看不清,换一个 以重新生成有机物.");
        ll.addView(tv2, lp_mw);

        moleculeView = new MoleculeView(this);
        moleculeView.setTextColor(ResUtils.skin_black.getDefaultColor());
        moleculeView.setMolecule(currMol);
        moleculeView.setOnClickListener(this);
        ll.addView(moleculeView, ViewBuilder.newLinearLayoutParams(MATCH_PARENT, WRAP_CONTENT,
                dip2px(this, 5), dip2px(this, 20), dip2px(this, 5), 0));
        tvSelectedCount = new TextView(this);
        tvSelectedCount.setTextColor(ResUtils.skin_black);
        tvSelectedCount.setTextSize(16);
        tvSelectedCount.setGravity(Gravity.CENTER);
        tvSelectedCount.setText("未选择");
        ll.addView(tvSelectedCount, lp_mw);

        int __10 = dip2px(this, 10);
        RelativeLayout hl = new RelativeLayout(this);
        reset = new TextView(this);
        reset.setTextColor(ResUtils.skin_black);
        reset.setTextSize(16);
        reset.setGravity(Gravity.CENTER);
        reset.setPadding(__10 * 2, 0, __10 * 2, __10 / 2);
        reset.setText("重置");
        reset.setTextColor(ResUtils.skin_blue);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moleculeView.unselectedAllChiral();
                Auth2Activity.this.onClick(moleculeView);
            }
        });
        hl.addView(reset, ViewBuilder.newRelativeLayoutParams(WRAP_CONTENT, WRAP_CONTENT,
                RelativeLayout.ALIGN_PARENT_LEFT, -1));
        newOne = new TextView(this);
        newOne.setTextColor(ResUtils.skin_black);
        newOne.setTextSize(16);
        newOne.setGravity(Gravity.CENTER);
        newOne.setPadding(__10 * 2, 0, __10 * 2, __10 / 2);
        newOne.setText("看不清,换一个");
        newOne.setTextColor(ResUtils.skin_blue);
        newOne.setOnClickListener(this);
        hl.addView(newOne, ViewBuilder.newRelativeLayoutParams(WRAP_CONTENT, WRAP_CONTENT,
                RelativeLayout.ALIGN_PARENT_RIGHT, -1));
        ll.addView(hl, ViewBuilder.newLinearLayoutParams(WRAP_CONTENT, WRAP_CONTENT,
                0, 0, 0, __10 * 2));

        nextStep = new Button(this);
        nextStep.setOnClickListener(this);
        ResUtils.applyStyleCommonBtnBlue(nextStep);
        nextStep.setText("下一步");
        ll.addView(nextStep, newLinearLayoutParams(MATCH_PARENT, WRAP_CONTENT, __10, __10 / 2, __10, __10 / 2));

        this.setContentView(bounceScrollView);
        setContentBackgroundDrawable(ResUtils.skin_background);
        setTitle("高级验证");
        TextView rightBtn = (TextView) getRightTextView();
        rightBtn.setVisibility(View.VISIBLE);
        rightBtn.setEnabled(true);
        rightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (LicenseStatus.getAuth2Status()) {
                    CustomDialog.create(Auth2Activity.this).setTitle("解除验证").setMessage("此操作将会解除验证, 是否继续?")
                            .setCancelable(true).setPositiveButton("确认", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            LicenseStatus.clearAuth2Status();
                            Utils.showToast(Auth2Activity.this, TOAST_TYPE_SUCCESS, "操作成功", Toast.LENGTH_LONG);
                            Auth2Activity.this.finish();
                        }
                    }).setNegativeButton("取消", null).show();
                } else {
                    Auth2Activity.this.finish();
                }
            }
        });
        if (LicenseStatus.getAuth2Status()) {
            rightBtn.setText("吊销");
            moleculeView.setMolecule(LicenseStatus.getAuth2Molecule());
            moleculeView.setSelectedChiral(LicenseStatus.getAuth2Chiral());
            moleculeView.setEnabled(false);
            newOne.setVisibility(View.GONE);
            reset.setVisibility(View.GONE);
            nextStep.setText("验证已完成");
            nextStep.setEnabled(false);
            onClick(moleculeView);
        } else {
            rightBtn.setText("取消");
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            onClick(newOne);
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        if (v == moleculeView) {
            int count = moleculeView.getSelectedChiralCount();
            if (count == 0) {
                tvSelectedCount.setText("未选择");
            } else {
                tvSelectedCount.setText("已选择: " + count);
            }
        } else if (v == newOne) {
            if (makingMol == null) {
                refreshId++;
                makingMol = (AlertDialog) CustomDialog.createFailsafe(this).setCancelable(false).setTitle("正在加载")
                        .setMessage("请稍候...(一般不会超过一分钟)")
                        .setNegativeButton("取消", this).show();
                new Thread(this).start();
            }
        } else if (v == nextStep) {
            if (moleculeView.getMolecule() == null) {
                showToast(Auth2Activity.this, TOAST_TYPE_INFO, "请先加载结构式(点\"换一个\")", 0);
                return;
            }
            if (moleculeView.getSelectedChiralCount() == 0) {
                showToast(Auth2Activity.this, TOAST_TYPE_INFO, "请选择手性碳原子", 0);
            } else {
                if (mChiralCarbons == null || mChiralCarbons.size() == 0) {
                    showToast(Auth2Activity.this, TOAST_TYPE_ERROR, "未知错误, 请重新加载结构式", 0);
                } else {
                    boolean pass = true;
                    HashSet<Integer> tmp = new HashSet<>(mChiralCarbons);
                    for (int i : moleculeView.getSelectedChiral()) {
                        if (tmp.contains(i)) {
                            tmp.remove(i);
                        } else {
                            pass = false;
                            break;
                        }
                    }
                    if (tmp.size() > 0) pass = false;
                    if (pass) {
                        LicenseStatus.setAuth2Status(moleculeView.getMolecule(), Utils.integerSetToArray(mChiralCarbons));
                        showToast(Auth2Activity.this, TOAST_TYPE_SUCCESS, "验证成功", 1);
                        moleculeView.setEnabled(false);
                        newOne.setVisibility(View.GONE);
                        reset.setVisibility(View.GONE);
                        nextStep.setText("验证已完成");
                        nextStep.setEnabled(false);
                        View rightBtn = getRightTextView();
                        if (rightBtn instanceof TextView) {
                            ((TextView) rightBtn).setText("吊销");
                        }
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
                    } else {
                        showToast(Auth2Activity.this, TOAST_TYPE_ERROR, "选择错误, 请重试", 0);
                    }
                }
            }
        }
    }

    @Override
    public void run() {
        int curr = refreshId;
        Molecule mol = null;
        HashSet<Integer> cc = null;
        do {
            try {
//                long t0 = System.currentTimeMillis();
                mol = PubChemStealer.nextRandomMolecule();
//                long t1 = System.currentTimeMillis();
//                if (mol != null) {
//                    log("nextRandomMolecule took " + (t1 - t0) + "ms");
//                }
                if (mol != null) {
                    cc = ChiralCarbonHelper.getMoleculeChiralCarbons(mol);
//                    long t2 = System.currentTimeMillis();
//                    log("getMoleculeChiralCarbons(" + mol.atomCount() + "atoms," + mol.bondCount() + "bonds) took " + (t2 - t1) + "ms");
                }
            } catch (RuntimeException e) {
                log(e);
            }
        } while (mol != null && curr == refreshId && cc != null && cc.size() < 6);
        final Molecule molecule = mol;
        if (makingMol != null) {
            makingMol.dismiss();
            makingMol = null;
        } else {
            return;
        }
        if (curr != refreshId) return;
        if (molecule != null) {
            final HashSet<Integer> finalCc = cc;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mChiralCarbons = finalCc;
                    moleculeView.setMolecule(molecule);
                    onClick(moleculeView);
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Utils.showToastShort(Auth2Activity.this, "加载失败");
                }
            });
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
        makingMol = null;
        refreshId++;
    }
}
