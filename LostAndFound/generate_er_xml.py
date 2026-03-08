import math
import html

# Translation Map
translation_map = {
    # Tables
    'users': '用户',
    'roles': '角色',
    'user_roles': '用户角色',
    'lost_item': '寻物启事',
    'found_item': '失物招领',
    'message': '消息通知',
    'map_point': '地图点位',
    'notice': '公告',
    'sys_config': '系统配置',

    # Common Columns
    'id': 'ID',
    'created_at': '创建时间',
    'updated_at': '更新时间',
    
    # User Columns
    'username': '用户名',
    'email': '邮箱',
    'password_hash': '密码哈希',
    'enabled': '是否启用',
    'audited': '是否审核',
    'nickname': '昵称',
    'department': '部门',
    'phone': '电话',
    'avatar': '头像',
    'bio': '简介',

    # Role Columns
    'code': '代码',
    'name': '名称', 

    # User Role Columns
    'user_id': '用户ID',
    'role_id': '角色ID',

    # Item Columns (Lost/Found)
    'title': '标题', 
    'category': '分类',
    'description': '描述',
    'location': '地点',
    'latitude': '纬度', 
    'longitude': '经度', 
    'image_url': '图片链接', 
    'thumbnail_url': '缩略图链接',
    'status': '状态',
    'lost_time': '丢失时间',
    'found_time': '拾获时间',
    'point_id': '地图点ID',
    'contact': '联系方式',
    'images': '多图',
    'audit_status': '审核状态',
    'claim_status': '认领状态',
    'claim_user_id': '认领用户ID',
    'claim_time': '认领时间',

    # Message Columns
    'content': '内容', 
    'type': '类型',
    'is_read': '是否已读',
    'related_id': '关联ID',

    # Sys Config Columns
    'cfg_key': '配置键',
    'cfg_value': '配置值'
}

tables = [
    ('users', ['id', 'username', 'email', 'password_hash', 'enabled', 'audited', 'created_at', 'nickname', 'department', 'phone', 'avatar', 'bio']),
    ('roles', ['id', 'code', 'name']),
    ('user_roles', ['user_id', 'role_id']),
    ('lost_item', ['id', 'title', 'category', 'description', 'location', 'latitude', 'longitude', 'image_url', 'thumbnail_url', 'status', 'user_id', 'created_at', 'updated_at', 'lost_time', 'point_id', 'contact', 'images', 'audit_status', 'claim_status', 'claim_user_id', 'claim_time']),
    ('found_item', ['id', 'title', 'category', 'description', 'location', 'latitude', 'longitude', 'image_url', 'thumbnail_url', 'status', 'user_id', 'created_at', 'updated_at', 'found_time', 'point_id', 'contact', 'images', 'audit_status', 'claim_status', 'claim_user_id', 'claim_time']),
    ('message', ['id', 'user_id', 'title', 'content', 'type', 'is_read', 'related_id', 'created_at']),
    ('map_point', ['id', 'name', 'latitude', 'longitude', 'description']),
    ('notice', ['id', 'title', 'content', 'updated_at', 'image_url']),
    ('sys_config', ['cfg_key', 'cfg_value'])
]

xml_header = '<?xml version="1.0" encoding="UTF-8"?>\n<mxfile host="Electron" modified="2024-05-23T00:00:00.000Z" agent="5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) draw.io/24.4.0 Chrome/124.0.6367.207 Electron/30.0.6 Safari/537.36" etag="abcdefg" version="24.4.0" type="device">\n  <diagram id="diagram_1" name="Page-1">\n    <mxGraphModel dx="1422" dy="762" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="827" pageHeight="1169" math="0" shadow="0">\n      <root>\n        <mxCell id="0" />\n        <mxCell id="1" parent="0" />\n'
xml_footer = '      </root>\n    </mxGraphModel>\n  </diagram>\n</mxfile>'

# Title
xml_body = '        <mxCell id="title" value="失物招领系统 - 实体属性图" style="text;html=1;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;whiteSpace=wrap;rounded=0;fontStyle=1;fontSize=20;" vertex="1" parent="1">\n          <mxGeometry x="200" y="20" width="600" height="40" as="geometry" />\n        </mxCell>\n'

# Layout configuration
grid_cols = 3
cell_width = 700  # Space allocated for one entity cluster
cell_height = 700
start_x = 50
start_y = 100

for i, (table_name, columns) in enumerate(tables):
    # Calculate center of the cluster
    col_idx = i % grid_cols
    row_idx = i // grid_cols
    center_x = start_x + col_idx * cell_width + cell_width / 2
    center_y = start_y + row_idx * cell_height + cell_height / 2
    
    # Entity Rectangle
    entity_w = 120
    entity_h = 60
    entity_id = f"entity_{table_name}"
    
    display_table_name = translation_map.get(table_name, table_name)
    
    # Using rounded rectangle as requested
    xml_body += f'        <mxCell id="{entity_id}" value="{display_table_name}" style="rounded=1;whiteSpace=wrap;html=1;fontStyle=1;fontSize=14;" vertex="1" parent="1">\n          <mxGeometry x="{center_x - entity_w/2}" y="{center_y - entity_h/2}" width="{entity_w}" height="{entity_h}" as="geometry" />\n        </mxCell>\n'
    
    # Attributes (Ellipses) arranged in a circle
    num_cols = len(columns)
    radius_x = 220 # Horizontal radius
    radius_y = 180 # Vertical radius
    
    for j, col_name in enumerate(columns):
        angle = (2 * math.pi * j) / num_cols
        # Position ellipse
        attr_w = 100
        attr_h = 40
        attr_x = center_x + radius_x * math.cos(angle) - attr_w / 2
        attr_y = center_y + radius_y * math.sin(angle) - attr_h / 2
        
        attr_id = f"attr_{table_name}_{col_name}"
        display_col_name = translation_map.get(col_name, col_name)
        
        # Ellipse style
        xml_body += f'        <mxCell id="{attr_id}" value="{display_col_name}" style="ellipse;whiteSpace=wrap;html=1;" vertex="1" parent="1">\n          <mxGeometry x="{attr_x}" y="{attr_y}" width="{attr_w}" height="{attr_h}" as="geometry" />\n        </mxCell>\n'
        
        # Edge from Entity to Attribute
        # Arrow pointing to attribute
        edge_id = f"edge_{table_name}_{col_name}"
        xml_body += f'        <mxCell id="{edge_id}" value="" style="endArrow=classic;html=1;exitX=0.5;exitY=0.5;exitDx=0;exitDy=0;entryX=0.5;entryY=0.5;entryDx=0;entryDy=0;" edge="1" parent="1" source="{entity_id}" target="{attr_id}">\n          <mxGeometry width="50" height="50" relative="1" as="geometry">\n            <mxPoint x="{center_x}" y="{center_y}" as="sourcePoint" />\n            <mxPoint x="{attr_x + attr_w/2}" y="{attr_y + attr_h/2}" as="targetPoint" />\n          </mxGeometry>\n        </mxCell>\n'

with open('er_output.xml', 'w', encoding='utf-8') as f:
    f.write(xml_header + xml_body + xml_footer)
print("XML generated in er_output.xml")
