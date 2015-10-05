package holder.vis;

import holder.GenericProblemInstance;
import holder.GenericSolution;
import holder.sbe.SolutionBorder;

import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSeparator;

public class KBorderSelector<P extends GenericProblemInstance, S extends GenericSolution> extends JPanel {
	private static final long serialVersionUID = 1L;
	public static final String BORDER = "border";
	public static final String FULLVIEW = "fullview";
	public static final String PSMAPVIEW = "psmapView";

	public static final Object AVERAGE_INTERSECT = "avgIntersect";
	public KBorderSelector(Set<SolutionBorder<P,S>> borders, ActionListener listener){
		setLayout(new GridLayout(0,1));

		JCheckBox fullView = new JCheckBox("full view");
		fullView.putClientProperty(FULLVIEW,Boolean.TRUE);
		fullView.addActionListener(listener);
		add(fullView);

		JCheckBox psmapView = new JCheckBox("PS Map view");
		psmapView.putClientProperty(PSMAPVIEW,Boolean.TRUE);
		psmapView.addActionListener(listener);
		add(psmapView);

		add(new JSeparator());

		JCheckBox avgInt = new JCheckBox("avg intersect");
		avgInt.putClientProperty(KBorderSelector.AVERAGE_INTERSECT,Boolean.TRUE);
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
	}
}
