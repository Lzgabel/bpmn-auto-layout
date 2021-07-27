package cn.lzgabel.layouter;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import cn.lzgabel.util.FlowInformation;
import org.activiti.bpmn.model.*;

import org.activiti.bpmn.model.Process;

public class SimpleOrthogonalFlowRouter {

	static private BpmnModel model;

	public static void routeFlows(BpmnModel model) {
		SimpleOrthogonalFlowRouter.model = model;

		List<FlowNode> flowNodes = gatherAllFlowNodes(model);
		List<List<GraphicInfo>> flowNodeGraphicInfoLists = new ArrayList<>();

		for (FlowNode flowNode : flowNodes) {
			for (SequenceFlow sequenceFlow : flowNode.getOutgoingFlows()) {
				List<GraphicInfo> giList = updateFlowGraphicInfo(sequenceFlow);
				if (giList != null)
					flowNodeGraphicInfoLists.add(giList);
			}
		}

		for (MessageFlow messageFlow : model.getMessageFlows().values()) {
			List<GraphicInfo> giList = updateFlowGraphicInfo(messageFlow);
			if (giList != null)
				flowNodeGraphicInfoLists.add(giList);
		}

		for (Process process : model.getProcesses()) {
			for (Artifact artifact : process.getArtifacts()) {
				if (artifact instanceof Association) {
					Association association = (Association) artifact;
					List<GraphicInfo> giList = updateFlowGraphicInfo(association);
					if (giList != null)
						flowNodeGraphicInfoLists.add(giList);
				}
			}
		}
	}

	private static List<GraphicInfo> updateFlowGraphicInfo(Object flowType) {

		FlowInformation fi = getFlowInformation(flowType);

		if (fi == null)
			return null;

		String iD = fi.iD;
		String sourceRef = fi.sourceRef;
		String targetRef = fi.targetRef;

		List<GraphicInfo> graphicInfoList = model.getFlowLocationGraphicInfo(iD);
		List<GraphicInfo> bends = new ArrayList<>();
		graphicInfoList.clear();

		GraphicInfo sourceFlowGI = new GraphicInfo();
		GraphicInfo sourceNodeGI = model.getGraphicInfo(sourceRef);
		FlowNode sourceNode = (FlowNode) model.getFlowElement(sourceRef);

		GraphicInfo targetFlowGI = new GraphicInfo();
		GraphicInfo targetNodeGI = model.getGraphicInfo(targetRef);
		FlowNode targetNode = (FlowNode) model.getFlowElement(targetRef);

		Point sourceNodeCenter = null;
		Point targetNodeCenter = null;

		/////////////////////////////////////////////////////
		// 获取 source、target 的中心点，根据中心的进行如下规则对比
		//										 source  target
		//
		//		s_x < t_x && s_y = t_y  -------> right    left
		//  		* 如果目标是gateway 	-------> right    left
		//		s_x < t_x && s_y > t_y  -------> top      left
		//  		* 如果目标是gateway  	-------> right    bottom
		//		s_x < t_x && s_y < t_y  -------> bottom   left
		//  		* 如果目标是gateway  	-------> right    top
		//		s_x = t_x && s_y > t_y  -------> top      bottom
		//		s_x = t_x && s_y < t_y  -------> bottom   top
		/////////////////////////////////////////////////////

		Point sourceCenterPoint = getCenterPoint(sourceNodeGI);
		Point targetCenterPoint = getCenterPoint(targetNodeGI);
		if (sourceCenterPoint.getX() < targetCenterPoint.getX() && sourceCenterPoint.getY() == targetCenterPoint.getY()) {
			sourceNodeCenter = getRightCenterPoint(sourceNodeGI);
			targetNodeCenter = getLeftCenterPoint(targetNodeGI);
			// 如果目标节点是 gateway --> right left
			if (targetNode instanceof Gateway) {
				sourceNodeCenter = getRightCenterPoint(sourceNodeGI);
				targetNodeCenter = getLeftCenterPoint(targetNodeGI);
			}
		}
		if (sourceCenterPoint.getX() < targetCenterPoint.getX() && sourceCenterPoint.getY() > targetCenterPoint.getY()) {
			sourceNodeCenter = getCenterTopPoint(sourceNodeGI);
			targetNodeCenter = getLeftCenterPoint(targetNodeGI);
			// 如果目标节点是 gateway --> right bottom
			if (targetNode instanceof Gateway) {
				sourceNodeCenter = getRightCenterPoint(sourceNodeGI);
				targetNodeCenter = getCenterBottomPoint(targetNodeGI);
			}
		}
		if (sourceCenterPoint.getX() < targetCenterPoint.getX() && sourceCenterPoint.getY() < targetCenterPoint.getY()) {
			sourceNodeCenter = getCenterBottomPoint(sourceNodeGI);
			targetNodeCenter = getLeftCenterPoint(targetNodeGI);

			// 如果目标节点是 gateway --> right top
			if (!(sourceNode instanceof Gateway) && targetNode instanceof Gateway) {
				sourceNodeCenter = getRightCenterPoint(sourceNodeGI);
				targetNodeCenter = getCenterTopPoint(targetNodeGI);
			}
		}

		if (sourceCenterPoint.getX() == targetCenterPoint.getX() && sourceCenterPoint.getY() > targetCenterPoint.getY()) {
			sourceNodeCenter = getCenterTopPoint(sourceNodeGI);
			targetNodeCenter = getCenterBottomPoint(targetNodeGI);
		}
		if (sourceCenterPoint.getX() == targetCenterPoint.getX() && sourceCenterPoint.getY() > targetCenterPoint.getY()) {
			sourceNodeCenter = getCenterBottomPoint(sourceNodeGI);
			targetNodeCenter = getCenterTopPoint(targetNodeGI);
		}



		if (model.getPool(sourceRef) != null) {
			sourceFlowGI.setX(targetNodeCenter.getX());

			if (sourceNodeGI.getY() < targetNodeGI.getY()) {
				sourceFlowGI.setY(sourceNodeGI.getY() + sourceNodeGI.getHeight());
				targetFlowGI.setY(targetNodeCenter.getY() - (targetNodeGI.getHeight() * 0.5));
			} else {
				sourceFlowGI.setY(sourceNodeGI.getY());
				targetFlowGI.setY(targetNodeCenter.getY() + (targetNodeGI.getHeight() * 0.5));
			}

			graphicInfoList.add(sourceFlowGI);

			targetFlowGI.setX(targetNodeCenter.getX());
			graphicInfoList.add(targetFlowGI);
			return null;
		} else if (model.getPool(targetRef) != null) {
			sourceFlowGI.setX(sourceNodeCenter.getX());

			graphicInfoList.add(sourceFlowGI);

			targetFlowGI.setX(sourceNodeCenter.getX());
			if (targetNodeGI.getY() < sourceNodeGI.getY()) {
				sourceFlowGI.setY(sourceNodeCenter.getY() - (sourceNodeGI.getHeight() * 0.5));
				targetFlowGI.setY(targetNodeGI.getY() + targetNodeGI.getHeight());
			} else {
				sourceFlowGI.setY(sourceNodeCenter.getY() + (sourceNodeGI.getHeight() * 0.5));
				targetFlowGI.setY(targetNodeGI.getY());
			}

			graphicInfoList.add(targetFlowGI);
			return null;
		} else if (sourceNode == null)
			return null;

		double sourceX = sourceNodeGI.getX(), sourceY = sourceNodeGI.getY();
		double targetX = targetNodeGI.getX(), targetY = targetNodeGI.getY();

		if (sourceNode instanceof BoundaryEvent) {
			sourceX = sourceNodeGI.getX() + sourceNodeGI.getWidth() / 2;
			sourceY = sourceNodeGI.getY() + sourceNodeGI.getHeight() / 2;

			targetY += targetNodeGI.getHeight() / 2;

			GraphicInfo bendGI = new GraphicInfo();
			bendGI.setX(sourceX);
			bendGI.setY(targetY);
			bends.add(bendGI);
		} else if (Math.abs(sourceNodeCenter.getY() - targetNodeCenter.getY()) < 3) {
			sourceY += sourceNodeGI.getHeight() / 2;
			if (sourceNodeCenter.getX() < targetNodeCenter.getX()) {
				sourceX += sourceNodeGI.getWidth();
				targetY += targetNodeGI.getHeight() / 2;
			}

		} else if (Math.abs(sourceNodeCenter.getX() - targetNodeCenter.getX()) < 3) {
			sourceX += sourceNodeGI.getWidth() / 2;
			if (sourceNodeCenter.getY() < targetNodeCenter.getY()) {
				sourceY += sourceNodeGI.getHeight();
				targetX += targetNodeGI.getWidth() / 2;
			} else {
				targetY += sourceNodeGI.getHeight();
				targetX += targetNodeGI.getWidth() / 2;
			}
		} else if (sourceNodeCenter.getY() > targetNodeCenter.getY()) {

			if (flowType instanceof MessageFlow) {

				int poolMiddleCoordinate = getPoolMiddleCoordinate((MessageFlow) flowType);
				int offset = 0;
				int direction = 1;

				while (horizontalLineIsOverlapping(poolMiddleCoordinate, sourceNodeCenter.x, targetNodeCenter.x, offset)) {

					if(direction == 1)
					{
						offset = (Math.abs(offset) + 5);
						direction *= -1;
					}else {
						offset = direction * offset;
						direction *= -1;
					}
				}

				GraphicInfo bend1 = new GraphicInfo();
				bend1.setX(sourceNodeCenter.x);
				bend1.setY(poolMiddleCoordinate + offset);
				bends.add(bend1);

				GraphicInfo bend2 = new GraphicInfo();
				bend2.setX(targetNodeCenter.x);
				bend2.setY(poolMiddleCoordinate + offset);
				bends.add(bend2);

			} else if (sourceNode.getOutgoingFlows().size() == 1) {
				sourceX += sourceNodeGI.getWidth();
				sourceY += sourceNodeGI.getHeight() / 2;
				targetX += targetNodeGI.getWidth() / 2;
				targetY += targetNodeGI.getHeight();
				GraphicInfo bendGI = new GraphicInfo();
				bendGI.setX(targetX);
				bendGI.setY(sourceY);
				bends.add(bendGI);
			} else if (sourceNode.getOutgoingFlows().size() > 1) {
				sourceX = sourceNodeGI.getX() + sourceNodeGI.getWidth() / 2;
				sourceY = sourceNodeGI.getY() + sourceNodeGI.getHeight() / 2;
				targetY += targetNodeGI.getHeight() / 2;
				GraphicInfo bendGI = new GraphicInfo();
				bendGI.setX(sourceX);
				bendGI.setY(targetY);
				bends.add(bendGI);
			}

		} else if (sourceNodeCenter.getY() < targetNodeCenter.getY()) {
			if (flowType instanceof MessageFlow) {
				int poolMiddleCoordinate = getPoolMiddleCoordinate((MessageFlow) flowType);
				int yOffset = 0;
				int yOffsetDirection = 1;

				while (horizontalLineIsOverlapping(poolMiddleCoordinate, sourceNodeCenter.x, targetNodeCenter.x, yOffset)) {
					yOffsetDirection *= -1;
					yOffset = yOffsetDirection * (Math.abs(yOffset) + 5);
				}
				GraphicInfo bend1 = new GraphicInfo();
				bend1.setX(sourceNodeCenter.x);
				bend1.setY(poolMiddleCoordinate + yOffset);
				bends.add(bend1);

				GraphicInfo bend2 = new GraphicInfo();
				bend2.setX(targetNodeCenter.x);
				bend2.setY(poolMiddleCoordinate + yOffset);
				bends.add(bend2);

			} else if (sourceNode.getOutgoingFlows().size() == 1) {
				sourceX += sourceNodeGI.getWidth();
				sourceY += sourceNodeGI.getHeight() / 2;
				targetX += targetNodeGI.getWidth() / 2;
				GraphicInfo bendGI = new GraphicInfo();
				bendGI.setX(targetX);
				bendGI.setY(sourceY);
				bends.add(bendGI);
			} else if (sourceNode.getOutgoingFlows().size() > 1) {
				sourceX = sourceNodeGI.getX() + sourceNodeGI.getWidth() / 2;
				sourceY = sourceNodeGI.getY() + sourceNodeGI.getHeight() / 2;
				targetY += targetNodeGI.getHeight() / 2;
				GraphicInfo bendGI = new GraphicInfo();
				bendGI.setX(sourceX);
				bendGI.setY(targetY);
				bends.add(bendGI);
			}

		}

		sourceFlowGI.setX(sourceNodeCenter.x);
		sourceFlowGI.setY(sourceNodeCenter.y);
		graphicInfoList.add(sourceFlowGI);

		graphicInfoList.addAll(bends);

		targetFlowGI.setX(targetNodeCenter.x);
		targetFlowGI.setY(targetNodeCenter.y);
		graphicInfoList.add(targetFlowGI);

		return graphicInfoList;
	}

	private static boolean horizontalLineIsOverlapping(int y, int x1, int x2, int yOffset) {

		for (MessageFlow messageFlow : model.getMessageFlows().values()) {

			List<GraphicInfo> graphicInfoList = model.getFlowLocationGraphicInfo(messageFlow.getId());

			int currentYCoordinate = y + yOffset;

			for (int i = 0; i < graphicInfoList.size() - 1; i++) {

				if (graphicInfoList.get(i).getY() == graphicInfoList.get(i + 1).getY() && currentYCoordinate == graphicInfoList.get(i).getY()) {
					if (graphicInfoList.get(i).getX() < graphicInfoList.get(i + 1).getX()) {
						if (x1 > graphicInfoList.get(i).getX() && x1 < graphicInfoList.get(i + 1).getX())
							return true;

						if (x2 > graphicInfoList.get(i).getX() && x2 < graphicInfoList.get(i + 1).getX())
							return true;
					}
					if (graphicInfoList.get(i).getX() > graphicInfoList.get(i + 1).getX()) {
						if (x1 < graphicInfoList.get(i).getX() && x1 > graphicInfoList.get(i + 1).getX())
							return true;

						if (x2 < graphicInfoList.get(i).getX() && x2 > graphicInfoList.get(i + 1).getX())
							return true;
					}
				}
			}
		}

		return false;
	}

	private static boolean verticalLineIsOverlapping(int x, int y1, int y2, int xOffset) {

		for (MessageFlow messageFlow : model.getMessageFlows().values()) {

			List<GraphicInfo> graphicInfoList = model.getFlowLocationGraphicInfo(messageFlow.getId());

			int currentXCoordinate = x + xOffset;

			for (int i = 0; i < graphicInfoList.size() - 1; i++) {

				if (graphicInfoList.get(i).getX() == graphicInfoList.get(i + 1).getX() && currentXCoordinate == graphicInfoList.get(i).getX()) {
					if (graphicInfoList.get(i).getY() < graphicInfoList.get(i + 1).getY()) {
						if (x > graphicInfoList.get(i).getY() && x < graphicInfoList.get(i + 1).getY())
							return true;

						if (y2 > graphicInfoList.get(i).getY() && y2 < graphicInfoList.get(i + 1).getY())
							return true;
					}
					if (graphicInfoList.get(i).getY() > graphicInfoList.get(i + 1).getY()) {
						if (x < graphicInfoList.get(i).getY() && x > graphicInfoList.get(i + 1).getY())
							return true;

						if (y2 < graphicInfoList.get(i).getY() && y2 > graphicInfoList.get(i + 1).getY())
							return true;
					}
				}
			}
		}

		return false;
	}

	private static FlowInformation getFlowInformation(Object flowType) {
		FlowInformation fi = new FlowInformation();

		if (flowType instanceof SequenceFlow) {
			SequenceFlow seqFlow = (SequenceFlow) flowType;
			fi.iD = seqFlow.getId();
			fi.sourceRef = seqFlow.getSourceRef();
			fi.targetRef = seqFlow.getTargetRef();
		}

		if (flowType instanceof MessageFlow) {
			MessageFlow msgFlow = (MessageFlow) flowType;
			fi.iD = msgFlow.getId();
			fi.sourceRef = msgFlow.getSourceRef();
			fi.targetRef = msgFlow.getTargetRef();
		}

		if (flowType instanceof Association) {
			Association association = (Association) flowType;
			fi.iD = association.getId();
			fi.sourceRef = association.getSourceRef();
			fi.targetRef = association.getTargetRef();

			if (!(model.getFlowElement(fi.sourceRef) instanceof FlowNode))
				return null;

			if (!(model.getFlowElement(fi.targetRef) instanceof FlowNode))
				return null;
		}
		return fi;
	}

	private static int getPoolMiddleCoordinate(MessageFlow flow) {
		Pool sourceNodePool = getPoolOfNode(flow.getSourceRef());
		Pool targetNodePool = getPoolOfNode(flow.getTargetRef());

		Lane sourceNodeLane = getLaneOfNode(flow.getSourceRef());
		Lane targetNodeLane = getLaneOfNode(flow.getTargetRef());

		GraphicInfo sourceNodeGI;
		GraphicInfo targetNodeGI;

		if (sourceNodePool != null) {
			sourceNodeGI = model.getGraphicInfo(sourceNodePool.getId());
		} else if (sourceNodeLane != null) {
			sourceNodeGI = model.getGraphicInfo(sourceNodeLane.getId());
		} else {
			return 0;
		}

		if (targetNodePool != null) {
			targetNodeGI = model.getGraphicInfo(targetNodePool.getId());
		} else if (targetNodeLane != null) {
			targetNodeGI = model.getGraphicInfo(targetNodeLane.getId());
		} else {
			return 0;
		}

		if (sourceNodeGI.getY() > targetNodeGI.getY()) {
			return (int) (sourceNodeGI.getY() - 60);
		} else {
			return (int) (targetNodeGI.getY() - 60);
		}
	}

	private static Lane getLaneOfNode(String iD) {

		for (Process process : model.getProcesses()) {
			for (Lane lane : process.getLanes()) {
				if (lane.getFlowReferences().contains(iD))
					return lane;
			}
		}
		return null;
	}

	private static Pool getPoolOfNode(String ref) {

		for (Pool pool : model.getPools()) {
			Process process = model.getProcess(pool.getId());

			if (process == null)
				continue;

			if (process.getFlowElement(ref) != null)
				return pool;

			for (SubProcess subprocess : process.findFlowElementsOfType(SubProcess.class)) {
				if (subprocess.getFlowElement(ref) != null)
					return pool;
			}
		}

		return null;
	}

	private static Point getCenterPoint(GraphicInfo sourceNodeGI) {
		int x = (int) (sourceNodeGI.getX() + sourceNodeGI.getWidth() / 2);
		int y = (int) (sourceNodeGI.getY() + sourceNodeGI.getHeight() / 2);
		return new Point(x, y);
	}
	private static Point getCenterTopPoint(GraphicInfo sourceNodeGI) {
		int x = (int) (sourceNodeGI.getX() + sourceNodeGI.getWidth() / 2);
		int y = (int) (sourceNodeGI.getY());
		return new Point(x, y);
	}

	private static Point getCenterBottomPoint(GraphicInfo sourceNodeGI) {
		int x = (int) (sourceNodeGI.getX() + sourceNodeGI.getWidth() / 2);
		int y = (int) (sourceNodeGI.getY()+ sourceNodeGI.getHeight());
		return new Point(x, y);
	}

	private static Point getRightCenterPoint(GraphicInfo sourceNodeGI) {
		int x = (int) (sourceNodeGI.getX() + sourceNodeGI.getWidth());
		int y = (int) (sourceNodeGI.getY() + sourceNodeGI.getHeight() / 2);
		return new Point(x, y);
	}

	private static Point getLeftCenterPoint(GraphicInfo sourceNodeGI) {
		int x = (int) (sourceNodeGI.getX());
		int y = (int) (sourceNodeGI.getY() + sourceNodeGI.getHeight() / 2);
		return new Point(x, y);
	}

	private static List<FlowNode> gatherAllFlowNodes(BpmnModel model) {
		List<FlowNode> flowNodes = new ArrayList<FlowNode>();
		for (Process process : model.getProcesses()) {
			flowNodes.addAll(gatherAllFlowNodes(process));
		}
		return flowNodes;
	}

	private static List<FlowNode> gatherAllFlowNodes(FlowElementsContainer flowElementsContainer) {
		List<FlowNode> flowNodes = new ArrayList<FlowNode>();
		for (FlowElement flowElement : flowElementsContainer.getFlowElements()) {
			if (flowElement instanceof FlowNode) {
				flowNodes.add((FlowNode) flowElement);
			}
			if (flowElement instanceof FlowElementsContainer) {
				flowNodes.addAll(gatherAllFlowNodes((FlowElementsContainer) flowElement));
			}
		}
		return flowNodes;
	}

}
