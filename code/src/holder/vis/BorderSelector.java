package holder.vis;

import holder.sbe.SolutionBorder;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSeparator;

public class BorderSelector extends JPanel {
	private static final long serialVersionUID = 1L;
	public static final String BORDER = "border";
	public static final String FULLVIEW = "fullview";
	public static final String PSMAPVIEW = "psmapView";

	public static final Object AVERAGE_INTERSECT = "avgIntersect";
	public BorderSelector(Set<SolutionBorder> borders, ActionListener listener){
		setLayout(new GridLayout(0,1));

		JCheckBox fullView = new JCheckBox("full view");
		fullView.putClientProperty(FULLVIEW,Boolean.TRUE);
		fullView.addActionListener(listener);
		add(fullView);

		final JCheckBox psmapView = new JCheckBox("PS Map view");
		psmapView.putClientProperty(PSMAPVIEW,Boolean.TRUE);
		psmapView.addActionListener(listener);
		add(psmapView);
		add(new JSeparator());

		JCheckBox avgInt = new JCheckBox("avg intersect");
		avgInt.putClientProperty(BorderSelector.AVERAGE_INTERSECT,Boolean.TRUE);
		avgInt.addActionListener(listener);
		add(avgInt);
		add(new JSeparator());
		if (borders == null){
			avgInt.setEnabled(false);
		}

		//commenting this out now b/c can't have lots of solution borders w/o making GUI unusable
//		if (borders != null){
//			JPanel borderCheckBoxes = new JPanel();
//			borderCheckBoxes.setLayout(new GridLayout(0,1));
//
//			for (SolutionBorder border : borders){
//				JCheckBox box = new JCheckBox("<html>" + border.getSolution().toString() + " vs<br>" + border.getNeighborSolution().toString()+"</html>");
//				box.putClientProperty(BORDER, border);
//				box.addActionListener(listener);
//				borderCheckBoxes.add(box);
//			}
//			add(new JScrollPane(borderCheckBoxes));
//
//		}

		this.addComponentListener(new ComponentAdapter(){
			@Override
			public void componentShown(ComponentEvent event){
				Component c = event.getComponent();
				c.removeComponentListener(this);
				psmapView.doClick();
			}
		});



	}
}
